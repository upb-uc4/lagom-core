package de.upb.cs.uc4.exam.impl

import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, EntityRef }
import akka.stream.Materializer
import akka.util.Timeout
import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport.{ MessageProtocol, ResponseHeader }
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import com.typesafe.config.Config
import de.upb.cs.uc4.authentication.model.AuthenticationRole
import de.upb.cs.uc4.certificate.api.CertificateService
import de.upb.cs.uc4.course.api.CourseService
import de.upb.cs.uc4.exam.api.ExamService
import de.upb.cs.uc4.exam.impl.actor.{ ExamBehaviour, ExamsWrapper }
import de.upb.cs.uc4.exam.impl.commands.{ GetExams, GetProposalAddExam }
import de.upb.cs.uc4.exam.model.Exam
import de.upb.cs.uc4.hyperledger.api.model.{ JsonHyperledgerVersion, UnsignedProposal }
import de.upb.cs.uc4.hyperledger.impl.{ HyperledgerUtils, ProposalWrapper }
import de.upb.cs.uc4.hyperledger.impl.commands.HyperledgerBaseCommand
import de.upb.cs.uc4.operation.api.OperationService
import de.upb.cs.uc4.operation.model.JsonOperationId
import de.upb.cs.uc4.shared.client.exceptions.{ DetailedError, ErrorType, UC4Exception, UC4NonCriticalException }
import de.upb.cs.uc4.shared.server.ServiceCallFactory._
import org.slf4j.{ Logger, LoggerFactory }
import play.api.Environment

import scala.concurrent.{ Await, ExecutionContext, TimeoutException }
import scala.concurrent.duration._

/** Implementation of the ExamService */
class ExamServiceImpl(
    clusterSharding: ClusterSharding,
    courseService: CourseService,
    certificateService: CertificateService,
    operationService: OperationService,
    override val environment: Environment
)(implicit ec: ExecutionContext, config: Config, materializer: Materializer)
  extends ExamService {

  /** Looks up the entity for the given ID */
  private def entityRef: EntityRef[HyperledgerBaseCommand] =
    clusterSharding.entityRefFor(ExamBehaviour.typeKey, ExamBehaviour.entityId)

  implicit val timeout: Timeout = Timeout(config.getInt("uc4.timeouts.hyperledger").milliseconds)

  lazy val validationTimeout: FiniteDuration = config.getInt("uc4.timeouts.validation").milliseconds

  private final val log: Logger = LoggerFactory.getLogger(classOf[ExamServiceImpl])

  /** This Methods needs to allow a GET-Method */
  override def allowVersionNumber: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET")

  /** Get the version of the Hyperledger API and the version of the chaincode the service uses */
  override def getHlfVersions: ServiceCall[NotUsed, JsonHyperledgerVersion] = ServiceCall { _ =>
    HyperledgerUtils.VersionUtil.createHyperledgerVersionResponse(entityRef)
  }

  /** Returns Exams, optionally filtered */
  override def getExams(examIds: Option[String], courseIds: Option[String], lecturerIds: Option[String], moduleIds: Option[String], types: Option[String], admittableAt: Option[String], droppableAt: Option[String]): ServiceCall[NotUsed, Seq[Exam]] = authenticated(AuthenticationRole.All: _*) {
    ServerServiceCall { (_, _) =>
      entityRef.askWithStatus[ExamsWrapper](replyTo => GetExams(splitOption(examIds), splitOption(courseIds), splitOption(lecturerIds),
        splitOption(moduleIds), splitOption(types), admittableAt, droppableAt, replyTo)).map{ wrapper =>
        (ResponseHeader(200, MessageProtocol.empty, List()), wrapper.exams)
      }.recover(handleException("getExams failed"))
    }
  }

  private def splitOption(option: Option[String]): Seq[String] = option.getOrElse("").trim.split(",")

  /** Get a proposal for adding an Exam */
  override def getProposalAddExam: ServiceCall[Exam, UnsignedProposal] = identifiedAuthenticated(AuthenticationRole.Lecturer) {
    (authUser, role) =>
      ServerServiceCall { (header, examRaw) =>

        val exam = examRaw.trim

        val validationList = try {
          Await.result(exam.validate, validationTimeout)
        }
        catch {
          case _: TimeoutException => throw UC4Exception.ValidationTimeout
          case e: Exception        => throw UC4Exception.InternalServerError("Validation Error", e.getMessage)
        }


        certificateService.getCertificate(authUser).handleRequestHeader(addAuthenticationHeader(header)).invoke().flatMap { jsonCertificate =>
          if (validationList.nonEmpty) {
            throw new UC4NonCriticalException(422, DetailedError(ErrorType.Validation, validationList))
          }
          else {
            entityRef.askWithStatus[ProposalWrapper](replyTo => GetProposalAddExam(jsonCertificate.certificate, exam, replyTo)).map {
              proposalWrapper =>
                operationService.addToWatchList(authUser).handleRequestHeader(addAuthenticationHeader(header)).invoke(JsonOperationId(proposalWrapper.operationId))
                  .recover {
                    case ex: UC4Exception if ex.possibleErrorResponse.`type` == ErrorType.OwnerMismatch =>
                    case throwable: Throwable =>
                      log.error("Exception in addToWatchlist getProposalAddExam", throwable)
                  }
                (ResponseHeader(200, MessageProtocol.empty, List()), UnsignedProposal(proposalWrapper.proposal))
            }.recover(handleException("getProposalAddExam failed"))
          }
        }
      }
  }

  /** Allows GET */
  override def allowedGet: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET")

  /** Allows POST */
  override def allowedPost: ServiceCall[NotUsed, Done] = allowedMethodsCustom("POST")
}
