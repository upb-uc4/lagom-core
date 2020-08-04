package de.upb.cs.uc4.matriculation.impl

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport.{MessageProtocol, ResponseHeader, TransportErrorCode}
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import de.upb.cs.uc4.authentication.api.AuthenticationService
import de.upb.cs.uc4.authentication.model.AuthenticationRole
import de.upb.cs.uc4.matriculation.api.MatriculationService
import de.upb.cs.uc4.matriculation.model.{ImmatriculationData, PutMessageMatriculationData, SubjectMatriculation}
import de.upb.cs.uc4.shared.client.exceptions.{CustomException, DetailedError}
import de.upb.cs.uc4.shared.server.ServiceCallFactory._
import de.upb.cs.uc4.shared.server.hyperledger.HyperLedgerSession
import de.upb.cs.uc4.user.api.UserService

import scala.concurrent.ExecutionContext

/** Implementation of the MatriculationService */
class MatriculationServiceImpl(hyperLedgerSession: HyperLedgerSession, userService: UserService)
                              (implicit ec: ExecutionContext, auth: AuthenticationService) extends MatriculationService {

  /** Immatriculates a student */
  override def addMatriculationData(username: String): ServiceCall[PutMessageMatriculationData, Done] =
    authenticated[PutMessageMatriculationData, Done](AuthenticationRole.Admin) {
      ServerServiceCall { (header, rawMessage) =>
        val message = rawMessage.trim
        val validationList = message.validate
        if (validationList.nonEmpty){
          throw new CustomException(TransportErrorCode(422, 1003, "Error"), DetailedError("validation error", validationList))
        }
        userService.getStudent(username).handleRequestHeader(_ => header).invoke()
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
                    (ResponseHeader(201, MessageProtocol.empty,
                      List(("Location", s"$pathPrefix/history/${student.username}"))), Done)
                  }
              }
          }
      }
    }

  /** Returns the ImmatriculationData of a student with the given username */
  override def getMatriculationData(username: String): ServiceCall[NotUsed, ImmatriculationData] =
      authenticated(AuthenticationRole.Admin) {
        ServerServiceCall { (header, _) =>
          userService.getStudent(username).handleRequestHeader(_ => header).invoke().flatMap { student =>
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
