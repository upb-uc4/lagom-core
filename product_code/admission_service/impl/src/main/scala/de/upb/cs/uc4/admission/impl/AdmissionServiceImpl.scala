package de.upb.cs.uc4.admission.impl

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, EntityRef }
import akka.stream.Materializer
import akka.util.Timeout
import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport.{ MessageProtocol, ResponseHeader }
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import com.typesafe.config.Config
import de.upb.cs.uc4.admission.api.AdmissionService
import de.upb.cs.uc4.admission.impl.actor.AdmissionBehaviour
import de.upb.cs.uc4.admission.impl.commands.{ GetCourseAdmissions, GetProposalForAddCourseAdmission, GetProposalForDropCourseAdmission }
import de.upb.cs.uc4.admission.model.{ CourseAdmission, DropAdmission }
import de.upb.cs.uc4.authentication.model.AuthenticationRole
import de.upb.cs.uc4.certificate.api.CertificateService
import de.upb.cs.uc4.course.api.CourseService
import de.upb.cs.uc4.examreg.api.ExamregService
import de.upb.cs.uc4.hyperledger.commands.{ HyperledgerBaseCommand, SubmitProposal }
import de.upb.cs.uc4.matriculation.api.MatriculationService
import de.upb.cs.uc4.shared.client.exceptions._
import de.upb.cs.uc4.shared.client.{ SignedTransactionProposal, TransactionProposal }
import de.upb.cs.uc4.shared.server.ServiceCallFactory._
import de.upb.cs.uc4.shared.server.messages.{ Accepted, Confirmation, Rejected }
import play.api.Environment

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future, TimeoutException }

/** Implementation of the MatriculationService */
class AdmissionServiceImpl(
    clusterSharding: ClusterSharding,
    matriculationService: MatriculationService,
    examregService: ExamregService,
    courseService: CourseService,
    certificateService: CertificateService,
    override val environment: Environment
)(implicit ec: ExecutionContext, config: Config, materializer: Materializer)
  extends AdmissionService {

  /** Looks up the entity for the given ID */
  private def entityRef: EntityRef[HyperledgerBaseCommand] =
    clusterSharding.entityRefFor(AdmissionBehaviour.typeKey, AdmissionBehaviour.entityId)

  implicit val timeout: Timeout = Timeout(config.getInt("uc4.timeouts.hyperledger").milliseconds)

  lazy val validationTimeout: FiniteDuration = config.getInt("uc4.timeouts.validation").milliseconds

  /** Returns course admissions */
  override def getCourseAdmissions(username: Option[String], courseId: Option[String], moduleId: Option[String]): ServiceCall[NotUsed, Seq[CourseAdmission]] =
    identifiedAuthenticated(AuthenticationRole.All: _*) { (authUser, role) =>
      ServerServiceCall { (header, _) =>

        val future = role match {
          case AuthenticationRole.Admin =>
            Future.successful(Done)

          case AuthenticationRole.Student if username.isDefined && username.get.trim == authUser =>
            Future.successful(Done)
          case AuthenticationRole.Student =>
            throw UC4Exception.OwnerMismatch

          case AuthenticationRole.Lecturer if courseId.isDefined =>
            courseService.findCourseByCourseId(courseId.get).handleRequestHeader(addAuthenticationHeader(header)).invoke().map { course =>
              if (course.lecturerId != authUser) {
                throw UC4Exception.OwnerMismatch
              }
              Done
            }
          case _ =>
            throw UC4Exception.NotEnoughPrivileges
        }

        future.flatMap { _ =>
          entityRef.askWithStatus[Seq[CourseAdmission]](replyTo => GetCourseAdmissions(username, courseId, moduleId, replyTo)).map {
            courseAdmissions =>
              createETagHeader(header, courseAdmissions)
          }.recover(handleException("Get course admission"))
        }
      }
    }

  /** Gets a proposal for adding a course admission */
  override def getProposalAddCourseAdmission: ServiceCall[CourseAdmission, TransactionProposal] = identifiedAuthenticated(AuthenticationRole.Student) {
    (authUser, _) =>
      {
        ServerServiceCall { (header, courseAdmissionRaw) =>

          val courseAdmissionTrimmed = courseAdmissionRaw.trim

          var validationList = try {
            Await.result(courseAdmissionTrimmed.validateOnCreation, validationTimeout)
          }
          catch {
            case _: TimeoutException => throw UC4Exception.ValidationTimeout
            case e: Exception        => throw UC4Exception.InternalServerError("Validation Error", e.getMessage)
          }

          certificateService.getEnrollmentId(authUser).handleRequestHeader(addAuthenticationHeader(header)).invoke().flatMap { jsonId =>

            //TODO : Proper timestamp
            val courseAdmissionFinalized = courseAdmissionTrimmed.copy(enrollmentId = jsonId.id, timestamp = LocalDateTime.now.format(DateTimeFormatter.ofPattern("YYYYMMdd_HHmmss")))

            courseService.findCourseByCourseId(courseAdmissionFinalized.courseId).handleRequestHeader(addAuthenticationHeader(header)).invoke().flatMap {
              course =>
                if (!course.moduleIds.contains(courseAdmissionFinalized.moduleId)) {
                  validationList :+= SimpleError("moduleId", "CourseId can not be attributed to module with the given moduleId.")
                  validationList :+= SimpleError("courseId", "The module with the given moduleId can not be attributed to the course with the given courseId.")
                }

                matriculationService.getMatriculationData(authUser).handleRequestHeader(addAuthenticationHeader(header)).invoke().flatMap {
                  data =>
                    //TODO Time logic, only in CURRENTLY active examregs
                    examregService.getExaminationRegulations(Some(data.matriculationStatus.map(_.fieldOfStudy).mkString(",")), None)
                      .handleRequestHeader(addAuthenticationHeader(header)).invoke().flatMap {
                        regulations =>
                          if (!regulations.flatMap(_.modules).map(_.id).contains(courseAdmissionFinalized.moduleId) && !validationList.map(_.name).contains("moduleId")) {
                            validationList :+= SimpleError("moduleId", "ModuleId can not be attributed to module with the given moduleId.")
                          }

                          if (validationList.nonEmpty) {
                            throw new UC4NonCriticalException(422, DetailedError(ErrorType.Validation, validationList))
                          }
                          else {
                            entityRef.askWithStatus[Array[Byte]](replyTo => GetProposalForAddCourseAdmission(courseAdmissionFinalized, replyTo)).map {
                              array => (ResponseHeader(200, MessageProtocol.empty, List()), TransactionProposal(array))
                            }.recover(handleException("Creation of add courseAdmission proposal failed"))
                          }
                      }
                }
            }
          }
        }
      }
  }

  /** Gets a proposal for dropping a course admission */
  override def getProposalDropCourseAdmission: ServiceCall[DropAdmission, TransactionProposal] = authenticated(AuthenticationRole.Student) {
    ServerServiceCall {
      (_, dropAdmissionRaw) =>
        {
          val dropAdmission = dropAdmissionRaw.trim
          val validationList = try {
            Await.result(dropAdmission.validateOnCreation, validationTimeout)
          }
          catch {
            case _: TimeoutException => throw UC4Exception.ValidationTimeout
            case e: Exception        => throw UC4Exception.InternalServerError("Validation Error", e.getMessage)
          }

          if (validationList.nonEmpty) {
            throw new UC4NonCriticalException(422, DetailedError(ErrorType.Validation, validationList))
          }

          entityRef.askWithStatus[Array[Byte]](replyTo => GetProposalForDropCourseAdmission(dropAdmission, replyTo)).map {
            array => (ResponseHeader(200, MessageProtocol.empty, List()), TransactionProposal(array))
          }.recover(handleException("Creation of drop courseAdmission proposal failed"))
        }
    }
  }

  /** Submits a proposal */
  override def submitProposal(): ServiceCall[SignedTransactionProposal, Done] = ServerServiceCall {
    (_, signedProposal) =>
      {
        entityRef.askWithStatus[Confirmation](replyTo => SubmitProposal(signedProposal, replyTo)).map {
          case Accepted(_) =>
            (ResponseHeader(202, MessageProtocol.empty, List()), Done)
          case Rejected(statusCode, reason) =>
            throw UC4Exception(statusCode, reason)
        }.recover(handleException("Submit proposal failed"))
      }
  }

  /** Allows GET */
  override def allowedGet: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET")

  /** Allows POST */
  override def allowedPost: ServiceCall[NotUsed, Done] = allowedMethodsCustom("POST")

  /** This Methods needs to allow a GET-Method */
  override def allowVersionNumber: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET")
}
