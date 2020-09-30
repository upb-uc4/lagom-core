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
import de.upb.cs.uc4.hyperledger.commands.HyperledgerCommand
import de.upb.cs.uc4.matriculation.api.MatriculationService
import de.upb.cs.uc4.matriculation.impl.actor.MatriculationBehaviour
import de.upb.cs.uc4.matriculation.impl.commands.{ AddEntriesToMatriculationData, AddMatriculationData, GetMatriculationData }
import de.upb.cs.uc4.matriculation.model.{ ImmatriculationData, PutMessageMatriculation }
import de.upb.cs.uc4.shared.client.Utils
import de.upb.cs.uc4.shared.client.exceptions.{ DetailedError, ErrorType, UC4Exception }
import de.upb.cs.uc4.shared.server.ServiceCallFactory._
import de.upb.cs.uc4.shared.server.messages.{ Accepted, Confirmation, RejectedWithError }
import de.upb.cs.uc4.user.api.UserService
import de.upb.cs.uc4.user.model.MatriculationUpdate
import de.upb.cs.uc4.user.model.user.Student

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, TimeoutException }
import scala.util.{ Failure, Success, Try }

/** Implementation of the MatriculationService */
class MatriculationServiceImpl(clusterSharding: ClusterSharding, userService: UserService)(implicit ec: ExecutionContext, config: Config, materializer: Materializer)
  extends MatriculationService {

  /** Looks up the entity for the given ID */
  private def entityRef: EntityRef[HyperledgerCommand] =
    clusterSharding.entityRefFor(MatriculationBehaviour.typeKey, MatriculationBehaviour.entityId)

  implicit val timeout: Timeout = Timeout(30.seconds)

  /** Immatriculates a student */
  override def addMatriculationData(username: String): ServiceCall[PutMessageMatriculation, Done] =
    authenticated[PutMessageMatriculation, Done](AuthenticationRole.Admin) {
      ServerServiceCall { (header, rawMessage) =>
        val message = rawMessage.trim

        // For syntax and regex checks, 5 seconds are more than enough
        val validationList = try {
          Await.result(message.validate, 5.seconds)
        }
        catch {
          case _: TimeoutException => throw CustomException.ValidationTimeout
          case e: Exception        => throw e
        }

        if (validationList.nonEmpty) {
          throw new UC4Exception(422, DetailedError(ErrorType.Validation, validationList))
        }
        userService.getUser(username).handleRequestHeader(addAuthenticationHeader(header)).invoke()
          .flatMap { user =>
            if (!user.isInstanceOf[Student]) {
              //We found a user, but it is not a Student. Therefore, a student with the username does not exist: NotFound
              throw UC4Exception.NotFound
            }
            val student = user.asInstanceOf[Student]

            entityRef.ask[Try[ImmatriculationData]](replyTo => GetMatriculationData(student.matriculationId, replyTo))
              .flatMap {
                case Success(_) =>
                  entityRef.ask[Confirmation](replyTo => AddEntriesToMatriculationData(
                    student.matriculationId,
                    message.matriculation,
                    replyTo
                  )).map {
                    case Accepted =>
                      userService.updateLatestMatriculation().invoke(MatriculationUpdate(
                        username,
                        Utils.findLatestSemester(message.matriculation.flatMap(_.semesters))
                      ))
                      (ResponseHeader(200, MessageProtocol.empty, List()), Done)

                    case RejectedWithError(statusCode, reason) => throw new UC4Exception(statusCode, reason)
                  }

                case Failure(exception) =>
                  exception match {
                    case customException: UC4Exception if customException.errorCode.http == 404 =>
                      val data = ImmatriculationData(
                        student.matriculationId, student.firstName, student.lastName, student.birthDate,
                        message.matriculation
                      )
                      entityRef.ask[Confirmation](replyTo => AddMatriculationData(data, replyTo)).map {
                        case Accepted =>
                          userService.updateLatestMatriculation().invoke(MatriculationUpdate(
                            username,
                            Utils.findLatestSemester(message.matriculation.flatMap(_.semesters))
                          ))
                          (ResponseHeader(201, MessageProtocol.empty, List(("Location", s"$pathPrefix/history/${student.username}"))), Done)

                        case RejectedWithError(statusCode, reason) => throw new UC4Exception(statusCode, reason)
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
            throw UC4Exception.OwnerMismatch
          }
          userService.getUser(username).handleRequestHeader(addAuthenticationHeader(header)).invoke().flatMap { user =>
            if (!user.isInstanceOf[Student]) {
              //We found a user, but it is not a Student. Therefore, a student with the username does not exist: NotFound
              throw UC4Exception.NotFound
            }
            val student = user.asInstanceOf[Student]
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
