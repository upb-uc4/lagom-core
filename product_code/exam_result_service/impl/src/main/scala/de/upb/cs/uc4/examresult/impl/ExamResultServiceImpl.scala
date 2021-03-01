package de.upb.cs.uc4.examresult.impl

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
import de.upb.cs.uc4.exam.api.ExamService
import de.upb.cs.uc4.examresult.api.ExamResultService
import de.upb.cs.uc4.examresult.impl.actor.{ ExamResultBehaviour, ExamResultWrapper }
import de.upb.cs.uc4.examresult.impl.commands.{ GetExamResultEntries, GetProposalAddExamResult }
import de.upb.cs.uc4.examresult.model.{ ExamResult, ExamResultEntry }
import de.upb.cs.uc4.hyperledger.api.model.{ JsonHyperledgerVersion, UnsignedProposal }
import de.upb.cs.uc4.hyperledger.impl.{ HyperledgerUtils, ProposalWrapper }
import de.upb.cs.uc4.hyperledger.impl.commands.HyperledgerBaseCommand
import de.upb.cs.uc4.operation.api.OperationService
import de.upb.cs.uc4.operation.model.JsonOperationId
import de.upb.cs.uc4.shared.client.exceptions.{ DetailedError, ErrorType, SimpleError, UC4Exception, UC4NonCriticalException }
import de.upb.cs.uc4.shared.server.ServiceCallFactory._
import org.slf4j.{ Logger, LoggerFactory }
import play.api.Environment

import scala.concurrent.{ Await, ExecutionContext, Future, TimeoutException }
import scala.concurrent.duration._

/** Implementation of the ExamResultService */
class ExamResultServiceImpl(
    clusterSharding: ClusterSharding,
    examService: ExamService,
    certificateService: CertificateService,
    operationService: OperationService,
    override val environment: Environment
)(implicit ec: ExecutionContext, config: Config, materializer: Materializer)
  extends ExamResultService {

  /** Looks up the entity for the given ID */
  private def entityRef: EntityRef[HyperledgerBaseCommand] =
    clusterSharding.entityRefFor(ExamResultBehaviour.typeKey, ExamResultBehaviour.entityId)

  implicit val timeout: Timeout = Timeout(config.getInt("uc4.timeouts.hyperledger").milliseconds)

  lazy val validationTimeout: FiniteDuration = config.getInt("uc4.timeouts.validation").milliseconds

  private final val log: Logger = LoggerFactory.getLogger(classOf[ExamResultServiceImpl])

  /** This Methods needs to allow a GET-Method */
  override def allowVersionNumber: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET")

  /** Get the version of the Hyperledger API and the version of the chaincode the service uses */
  override def getHlfVersions: ServiceCall[NotUsed, JsonHyperledgerVersion] = ServiceCall { _ =>
    HyperledgerUtils.VersionUtil.createHyperledgerVersionResponse(entityRef)
  }

  /** Returns ExamResultEntries, optionally filtered */
  override def getExamResults(username: Option[String], examIds: Option[String]): ServiceCall[NotUsed, Seq[ExamResultEntry]] = identifiedAuthenticated(AuthenticationRole.All: _*) {
    (authUser, role) =>
      ServerServiceCall { (header, _) =>
        {

          val authErrorFuture = role match {
            case AuthenticationRole.Admin =>
              Future.successful(Done)
            case AuthenticationRole.Lecturer =>
              if (examIds.isEmpty) {
                throw UC4Exception.OwnerMismatch
              }
              else {
                certificateService.getEnrollmentIds(Some(authUser)).handleRequestHeader(addAuthenticationHeader(header)).invoke().flatMap {
                  usernameEnrollmentIdPairSeq =>
                    val authEnrollmentId = usernameEnrollmentIdPairSeq.head.enrollmentId

                    examService.getExams(examIds, None, None, None, None, None, None).handleRequestHeader(addAuthenticationHeader(header)).invoke().map {
                      exams =>
                        if (exams.forall(exam => exam.lecturerEnrollmentId == authEnrollmentId)) {
                          Done
                        }
                        else {
                          throw UC4Exception.OwnerMismatch
                        }
                    }
                }
              }
            case AuthenticationRole.Student if username.isEmpty || username.get.trim != authUser =>
              throw UC4Exception.OwnerMismatch
            case AuthenticationRole.Student =>
              Future.successful(Done)
            case _ => throw UC4Exception.InternalServerError("Error checking authRole", s"Role is not one of Student,Lecturer or Admin but instead ${role}")
          }
          authErrorFuture.flatMap {
            _ =>

              val optEnrollmentId = username match {
                case Some(username) =>
                  certificateService.getEnrollmentIds(Some(username)).handleRequestHeader(addAuthenticationHeader(header)).invoke()
                    .map {
                      usernameEnrollmentIdPairSeq =>
                        usernameEnrollmentIdPairSeq.find(pair => pair.username == username) match {
                          case Some(pair) => Some(pair.enrollmentId)
                          case None       => throw UC4Exception.NotFound
                        }
                    }
                case None =>
                  Future.successful(None)
              }

              optEnrollmentId.flatMap { id =>
                entityRef.askWithStatus[ExamResultWrapper](replyTo => GetExamResultEntries(id, examIds.map(_.trim.split(",")), replyTo)).map {
                  examResultEntries =>
                    (ResponseHeader(200, MessageProtocol.empty, List()), examResultEntries.examResults)
                }.recover(handleException("getExamResults failed"))
              }
          }
        }
      }
  }

  /** Get a proposals for adding the result of an exam */
  override def getProposalAddExamResult: ServiceCall[ExamResult, UnsignedProposal] = identifiedAuthenticated(AuthenticationRole.Lecturer) {
    (authUser, _) =>
      ServerServiceCall { (header, examResultRaw) =>
        {

          val examResult = examResultRaw.trim

          val validationList = try {
            Await.result(examResult.validate, validationTimeout)
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
              entityRef.askWithStatus[ProposalWrapper](replyTo => GetProposalAddExamResult(jsonCertificate.certificate, examResult, replyTo)).map {
                proposalWrapper =>
                  operationService.addToWatchList(authUser).handleRequestHeader(addAuthenticationHeader(header)).invoke(JsonOperationId(proposalWrapper.operationId))
                    .recover {
                      case ex: UC4Exception if ex.possibleErrorResponse.`type` == ErrorType.OwnerMismatch =>
                      case throwable: Throwable =>
                        log.error("Exception in addToWatchlist getProposalAddExamResult", throwable)
                    }
                  (ResponseHeader(200, MessageProtocol.empty, List()), UnsignedProposal(proposalWrapper.proposal))
              }.recover(handleException("getExamResults failed"))
            }
          }

        }
      }
  }

  /** Allows GET */
  override def allowedGet: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET")

  /** Allows POST */
  override def allowedPost: ServiceCall[NotUsed, Done] = allowedMethodsCustom("POST")
}
