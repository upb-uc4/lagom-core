package de.upb.cs.uc4.authentication.impl

import akka.{Done, NotUsed}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.util.Timeout
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport._
import com.lightbend.lagom.scaladsl.persistence.ReadSide
import de.upb.cs.uc4.authentication.api.AuthenticationService
import de.upb.cs.uc4.authentication.impl.actor.{AuthenticationEntry, AuthenticationState}
import de.upb.cs.uc4.authentication.impl.commands.{AuthenticationCommand, GetAuthentication}
import de.upb.cs.uc4.authentication.impl.readside.AuthenticationEventProcessor
import de.upb.cs.uc4.authentication.model.AuthenticationRole.AuthenticationRole
import de.upb.cs.uc4.shared.client.exceptions.{CustomException, GenericError}
import de.upb.cs.uc4.shared.server.Hashing
import de.upb.cs.uc4.shared.server.ServiceCallFactory._

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
}
