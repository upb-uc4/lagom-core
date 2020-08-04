package de.upb.cs.uc4.authentication.impl

import akka.{Done, NotUsed}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.util.Timeout
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport._
import com.lightbend.lagom.scaladsl.persistence.ReadSide
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import de.upb.cs.uc4.authentication.api.AuthenticationService
import de.upb.cs.uc4.authentication.impl.actor.{AuthenticationEntry, AuthenticationState}
import de.upb.cs.uc4.authentication.impl.commands.{AuthenticationCommand, GetAuthentication, SetAuthentication}
import de.upb.cs.uc4.authentication.impl.readside.AuthenticationEventProcessor
import de.upb.cs.uc4.authentication.model.AuthenticationRole.AuthenticationRole
import de.upb.cs.uc4.authentication.model.{AuthenticationRole, AuthenticationUser}
import de.upb.cs.uc4.shared.client.exceptions.{CustomException, DetailedError, GenericError, SimpleError}
import de.upb.cs.uc4.shared.server.Hashing
import de.upb.cs.uc4.shared.server.ServiceCallFactory._
import de.upb.cs.uc4.shared.server.messages.{Accepted, Confirmation, RejectedWithError}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class AuthenticationServiceImpl(readSide: ReadSide, processor: AuthenticationEventProcessor,
                                clusterSharding: ClusterSharding)
                               (implicit ec: ExecutionContext) extends AuthenticationService {

  readSide.register(processor)


  private def entityRef(id: String): EntityRef[AuthenticationCommand] =
    clusterSharding.entityRefFor(AuthenticationState.typeKey, id)

  implicit val timeout: Timeout = Timeout(5.seconds)

  /** @inheritdoc */
  override def check(username: String, password: String): ServiceCall[NotUsed, (String, AuthenticationRole)] = ServiceCall { _ =>
    entityRef(Hashing.sha256(username)).ask[Option[AuthenticationEntry]](replyTo => GetAuthentication(replyTo)).map{
      case Some(entry) =>
        if (entry.password != Hashing.sha256(entry.salt + password)) {
          throw new CustomException(TransportErrorCode(401, 1003, "Unauthorized"),
            GenericError("authorization error"))
        } else {
          (username, entry.role)
        }
      case None =>
        throw new CustomException(TransportErrorCode(401, 1003, "Unauthorized"),
          GenericError("authorization error"))
    }
  }

  override def allowVersionNumber: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET")

  /** Sets the authentication data of a user */
  override def setAuthentication(): ServiceCall[AuthenticationUser, Done] = ServiceCall { user =>
    entityRef(Hashing.sha256(user.username)).ask[Confirmation](replyTo => SetAuthentication(user, replyTo)).map {
      case Accepted => Done
      case RejectedWithError(code, errorResponse) =>
        throw new CustomException(TransportErrorCode(code, 1003, "Error"), errorResponse)
    }
  }

  /** Changes the password of the given user */
  override def changePassword(username: String): ServiceCall[AuthenticationUser, Done] =
    identifiedAuthenticated[AuthenticationUser, Done](AuthenticationRole.All: _*) {
      (authUsername, role) =>
        ServerServiceCall{ (_: RequestHeader, user: AuthenticationUser) =>
          if (username != user.username.trim) {
            throw new CustomException(TransportErrorCode(400, 1003, "Error"), GenericError("path parameter mismatch"))
          }
          if (authUsername != user.username.trim) {
            throw new CustomException(TransportErrorCode(403, 1003, "Error"), GenericError("owner mismatch"))
          }
          if (role != user.role) {
            throw new CustomException(TransportErrorCode(422, 1003, "Error"), DetailedError("uneditable fields", List(SimpleError("role", "Role may not be manually changed."))))
          }
          val ref = entityRef(Hashing.sha256(user.username))

          ref.ask[Confirmation](replyTo => SetAuthentication(user, replyTo))
            .map {
              case Accepted => // Update Successful
                (ResponseHeader(200, MessageProtocol.empty, List()), Done)
              case RejectedWithError(code, errorResponse) =>
                throw new CustomException(TransportErrorCode(code, 1003, "Error"), errorResponse)
            }
        }
    }(this, ec)

  /** Allows PUT */
  override def allowedPut: ServiceCall[NotUsed, Done] = allowedMethodsCustom("PUT")
}
