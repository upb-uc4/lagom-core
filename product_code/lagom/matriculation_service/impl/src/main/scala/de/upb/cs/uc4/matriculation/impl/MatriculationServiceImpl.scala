package de.upb.cs.uc4.matriculation.impl

import akka.actor.ActorRef
import akka.stream.scaladsl.Source
import akka.stream.{CompletionStrategy, Materializer, OverflowStrategy}
import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.transport.{MessageProtocol, RequestHeader, ResponseHeader}
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import de.upb.cs.uc4.authentication.api.AuthenticationService
import de.upb.cs.uc4.authentication.model.AuthenticationRole
import de.upb.cs.uc4.matriculation.api.MatriculationService
import de.upb.cs.uc4.matriculation.model.{ImmatriculationData, PutMessageMatriculationData, SubjectMatriculation}
import de.upb.cs.uc4.shared.client.exceptions.{CustomException, DetailedError}
import de.upb.cs.uc4.shared.server.ServiceCallFactory._
import de.upb.cs.uc4.shared.server.hyperledger.HyperLedgerSession
import de.upb.cs.uc4.user.api.UserService
import de.upb.cs.uc4.user.model.MatriculationUpdate

import scala.concurrent.ExecutionContext

/** Implementation of the MatriculationService */
class MatriculationServiceImpl(hyperLedgerSession: HyperLedgerSession, userService: UserService)
                              (implicit ec: ExecutionContext, auth: AuthenticationService, materializer: Materializer)
  extends MatriculationService {

  def getAuthHeader(serviceHeader: RequestHeader): RequestHeader => RequestHeader = {
    origin => origin.addHeader("authorization", serviceHeader.headerMap("authorization").head._2)
  }

  /** Immatriculates a student */
  override def addMatriculationData(username: String): ServiceCall[PutMessageMatriculationData, Done] =
    authenticated[PutMessageMatriculationData, Done](AuthenticationRole.Admin) {
      ServerServiceCall { (header, rawMessage) =>
        val message = rawMessage.trim
        val validationList = message.validate
        if (validationList.nonEmpty) {
          throw new CustomException(422, DetailedError("validation error", validationList))
        }
        userService.getStudent(username).handleRequestHeader(getAuthHeader(header)).invoke()
          .flatMap { student =>
            hyperLedgerSession.read[ImmatriculationData]("getMatriculationData", student.matriculationId)
              .flatMap { _ =>
                hyperLedgerSession.write[String](
                  "addEntryToMatriculationData", Seq(
                    student.matriculationId,
                    message.fieldOfStudy,
                    message.semester
                  )
                ).map { _ =>
                  userService.updateLatestMatriculation().invoke(MatriculationUpdate(username, message.semester))
                  (ResponseHeader(200, MessageProtocol.empty, List()), Done)
                }
              }
              .recoverWith {
                case _: Exception =>
                  val data = ImmatriculationData(
                    student.matriculationId, student.firstName, student.lastName, student.birthDate,
                    Seq(SubjectMatriculation(message.fieldOfStudy, Seq(message.semester)))
                  )
                  hyperLedgerSession.write[ImmatriculationData]("addMatriculationData", data).map { _ =>
                    userService.updateLatestMatriculation().invoke(MatriculationUpdate(username, message.semester))
                    (ResponseHeader(201, MessageProtocol.empty,
                      List(("Location", s"$pathPrefix/history/${student.username}"))), Done)
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
          userService.getStudent(username).handleRequestHeader(getAuthHeader(header)).invoke().flatMap { student =>
            hyperLedgerSession.read[ImmatriculationData]("getMatriculationData", student.matriculationId).map {
              data => (ResponseHeader(200, MessageProtocol.empty, List()), data)
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
