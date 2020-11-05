package de.upb.cs.uc4.matriculation.impl

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
import de.upb.cs.uc4.hyperledger.commands.{ HyperledgerCommand, SubmitProposal }
import de.upb.cs.uc4.matriculation.api.MatriculationService
import de.upb.cs.uc4.matriculation.impl.actor.MatriculationBehaviour
import de.upb.cs.uc4.matriculation.impl.commands.{ GetMatriculationData, GetProposalForAddEntriesToMatriculationData, GetProposalForAddMatriculationData }
import de.upb.cs.uc4.matriculation.model.{ ImmatriculationData, PutMessageMatriculation }
import de.upb.cs.uc4.shared.client.exceptions.{ DetailedError, ErrorType, UC4Exception, UC4NonCriticalException }
import de.upb.cs.uc4.shared.client.{ SignedTransactionProposal, TransactionProposal }
import de.upb.cs.uc4.shared.client.Utils
import de.upb.cs.uc4.shared.client.exceptions.{ DetailedError, ErrorType, UC4Exception, UC4NonCriticalException }
import de.upb.cs.uc4.shared.server.ServiceCallFactory._
import de.upb.cs.uc4.shared.server.messages.{ Accepted, Confirmation, RejectedWithError }
import de.upb.cs.uc4.user.api.UserService
import de.upb.cs.uc4.user.model.user.Student

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, TimeoutException }
import scala.util.{ Failure, Success, Try }

/** Implementation of the MatriculationService */
class MatriculationServiceImpl(clusterSharding: ClusterSharding, userService: UserService, certificateService: CertificateService)(implicit ec: ExecutionContext, config: Config, materializer: Materializer)
  extends MatriculationService {

  /** Looks up the entity for the given ID */
  private def entityRef: EntityRef[HyperledgerCommand] =
    clusterSharding.entityRefFor(MatriculationBehaviour.typeKey, MatriculationBehaviour.entityId)

  implicit val timeout: Timeout = Timeout(30.seconds)

  lazy val validationTimeout: FiniteDuration = config.getInt("uc4.validation.timeout").milliseconds

  /** Submits a proposal to matriculate a student */
  override def submitMatriculationProposal(username: String): ServiceCall[SignedTransactionProposal, Done] =
    identifiedAuthenticated[SignedTransactionProposal, Done](AuthenticationRole.Student) { (authUser, _) =>
      ServerServiceCall { (_, message) =>
        if (authUser != username.trim) {
          throw UC4Exception.OwnerMismatch
        }

        entityRef.ask[Confirmation](replyTo => SubmitProposal(message, replyTo)).map {
          case Accepted =>
            (ResponseHeader(202, MessageProtocol.empty, List()), Done)
          case RejectedWithError(statusCode, reason) =>
            throw UC4Exception(statusCode, reason)
        }
      }
    }

  /** Get proposal to matriculate a student */
  override def getMatriculationProposal(username: String): ServiceCall[PutMessageMatriculation, TransactionProposal] =
    identifiedAuthenticated[PutMessageMatriculation, TransactionProposal](AuthenticationRole.Student) { (authUser, _) =>
      ServerServiceCall { (header, rawMessage) =>

        if (authUser != username.trim) {
          throw UC4Exception.OwnerMismatch
        }

        val message = rawMessage.trim

        val validationList = try {
          Await.result(message.validate, validationTimeout)
        }
        catch {
          case _: TimeoutException => throw UC4Exception.ValidationTimeout
          case e: Exception        => throw UC4Exception.InternalServerError("Validation Error", e.getMessage)
        }

        if (validationList.nonEmpty) {
          throw new UC4NonCriticalException(422, DetailedError(ErrorType.Validation, validationList))
        }

        certificateService.getEnrollmentId(username).handleRequestHeader(addAuthenticationHeader(header)).invoke()
          .flatMap { jsonEnrollmentId =>
            val enrollmentId = jsonEnrollmentId.id

            entityRef.ask[Try[ImmatriculationData]](replyTo => GetMatriculationData(enrollmentId, replyTo))
              .flatMap {
                case Success(_) =>
                  entityRef.ask[Try[Array[Byte]]](replyTo => GetProposalForAddEntriesToMatriculationData(
                    enrollmentId,
                    message.matriculation,
                    replyTo
                  )).map {
                    case Success(array) =>
                      (ResponseHeader(200, MessageProtocol.empty, List()), TransactionProposal(array))

                    case Failure(error) => throw error
                  }

                case Failure(exception) =>
                  exception match {
                    case uc4Exception: UC4Exception if uc4Exception.errorCode.http == 404 =>
                      val data = ImmatriculationData(
                        enrollmentId,
                        message.matriculation
                      )
                      entityRef.ask[Try[Array[Byte]]](replyTo => GetProposalForAddMatriculationData(data, replyTo)).map {
                        case Success(array) =>
                          (ResponseHeader(200, MessageProtocol.empty, List()), TransactionProposal(array))

                        case Failure(error) => throw error
                      }

                    case uc4Exception: UC4Exception => throw uc4Exception

                    case ex: Throwable =>
                      throw UC4Exception.InternalServerError("Failure at addition of new matriculation data", ex.getMessage, ex)
                  }
              }
          }
      }
    }

  /** Returns the ImmatriculationData of a student with the given username */
  override def getMatriculationData(username: String): ServiceCall[NotUsed, ImmatriculationData] =
    identifiedAuthenticated(AuthenticationRole.Admin, AuthenticationRole.Student) {
      (authUsername, role) =>
        ServerServiceCall { (header, _) =>
          if (role != AuthenticationRole.Admin && authUsername != username) {
            throw UC4Exception.OwnerMismatch
          }
          userService.getUser(username).handleRequestHeader(addAuthenticationHeader(header)).invoke().flatMap { user =>
            if (!user.isInstanceOf[Student]) {
              //We found a user, but it is not a Student. Therefore, a student with the username does not exist: NotFound
              throw UC4Exception.NotFound
            }
            certificateService.getEnrollmentId(username).handleRequestHeader(addAuthenticationHeader(header)).invoke()
              .flatMap { jsonEnrollmentId =>
                val enrollmentId = jsonEnrollmentId.id
                entityRef.ask[Try[ImmatriculationData]](replyTo => GetMatriculationData(enrollmentId, replyTo)).map {
                  case Success(data)      => createETagHeader(header, data)
                  case Failure(exception) => throw exception
                }
              }
          }
        }
    }

  /** Allows GET */
  override def allowedGet: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET")

  /** Allows PUT */
  override def allowedPost: ServiceCall[NotUsed, Done] = allowedMethodsCustom("POST")

  /** This Methods needs to allow a GET-Method */
  override def allowVersionNumber: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET")
}
