package de.upb.cs.uc4.matriculation.impl

import akka.stream.Materializer
import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport.{ MessageProtocol, RequestHeader, ResponseHeader }
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import de.upb.cs.uc4.authentication.api.AuthenticationService
import de.upb.cs.uc4.authentication.model.AuthenticationRole
import de.upb.cs.uc4.matriculation.api.MatriculationService
import de.upb.cs.uc4.matriculation.model.{ ImmatriculationData, PutMessageMatriculationData, SubjectMatriculation }
import de.upb.cs.uc4.shared.client.exceptions.{ CustomException, DetailedError }
import de.upb.cs.uc4.shared.server.ServiceCallFactory._
import de.upb.cs.uc4.user.api.UserService
import de.upb.cs.uc4.user.model.MatriculationUpdate
import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, EntityRef }
import akka.util.Timeout
import de.upb.cs.uc4.hyperledger.commands.HyperledgerCommand
import de.upb.cs.uc4.matriculation.impl.actor.MatriculationBehaviour
import de.upb.cs.uc4.matriculation.impl.commands.{ AddEntryToMatriculationData, AddMatriculationData, GetMatriculationData }
import de.upb.cs.uc4.shared.server.messages.{ Accepted, Confirmation, RejectedWithError }

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.util.{ Failure, Success, Try }

/** Implementation of the MatriculationService */
class MatriculationServiceImpl(clusterSharding: ClusterSharding, userService: UserService)(implicit ec: ExecutionContext, auth: AuthenticationService, materializer: Materializer)
  extends MatriculationService {

  def addAuthenticationHeader(serviceHeader: RequestHeader): RequestHeader => RequestHeader = {
    origin => origin.addHeader("authorization", serviceHeader.headerMap("authorization").head._2)
  }

  /** Looks up the entity for the given ID */
  private def entityRef: EntityRef[HyperledgerCommand] =
    clusterSharding.entityRefFor(MatriculationBehaviour.typeKey, MatriculationBehaviour.entityId)

  implicit val timeout: Timeout = Timeout(15.seconds)

  /** Immatriculates a student */
  override def addMatriculationData(username: String): ServiceCall[PutMessageMatriculationData, Done] =
    authenticated[PutMessageMatriculationData, Done](AuthenticationRole.Admin) {
      ServerServiceCall { (header, rawMessage) =>
        val message = rawMessage.trim
        val validationList = message.validate
        if (validationList.nonEmpty) {
          throw new CustomException(422, DetailedError("validation error", validationList))
        }
        userService.getStudent(username).handleRequestHeader(addAuthenticationHeader(header)).invoke()
          .flatMap { student =>
            entityRef.ask[Try[ImmatriculationData]](replyTo => GetMatriculationData(student.matriculationId, replyTo))
              .flatMap {
                case Success(_) =>
                  entityRef.ask[Confirmation](replyTo => AddEntryToMatriculationData(
                    student.matriculationId,
                    message.fieldOfStudy,
                    message.semester,
                    replyTo
                  )).map {
                    case Accepted =>
                      userService.updateLatestMatriculation().invoke(MatriculationUpdate(username, message.semester))
                      (ResponseHeader(200, MessageProtocol.empty, List()), Done)
                    case RejectedWithError(statusCode, reason) => throw new CustomException(statusCode, reason)
                  }
                case Failure(exception) =>
                  exception match {
                    case customException: CustomException if customException.getErrorCode.http == 404 =>
                      val data = ImmatriculationData(
                        student.matriculationId, student.firstName, student.lastName, student.birthDate,
                        Seq(SubjectMatriculation(message.fieldOfStudy, Seq(message.semester)))
                      )
                      entityRef.ask[Confirmation](replyTo => AddMatriculationData(data, replyTo)).map {
                        case Accepted =>
                          userService.updateLatestMatriculation().invoke(MatriculationUpdate(username, message.semester))
                          (ResponseHeader(201, MessageProtocol.empty, List(("Location", s"$pathPrefix/history/${student.username}"))), Done)
                        case RejectedWithError(statusCode, reason) => throw new CustomException(statusCode, reason)
                      }
                    case exception: Exception => throw exception
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
            throw CustomException.OwnerMismatch
          }
          userService.getStudent(username).handleRequestHeader(addAuthenticationHeader(header)).invoke().flatMap { student =>
            entityRef.ask[Try[ImmatriculationData]](replyTo => GetMatriculationData(student.matriculationId, replyTo)).map {
              case Success(data)      => (ResponseHeader(200, MessageProtocol.empty, List()), data)
              case Failure(exception) => throw exception
            }
          }
        }
    }

  /** Allows GET */
  override def allowedGet: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET")

  /** Allows PUT */
  override def allowedPut: ServiceCall[NotUsed, Done] = allowedMethodsCustom("PUT")

  /** This Methods needs to allow a GET-Method */
  override def allowVersionNumber: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET")

}
