package de.upb.cs.uc4.authentication.impl

import akka.Done
import akka.cluster.sharding.typed.scaladsl.Entity
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.persistence.jdbc.JdbcPersistenceComponents
import com.lightbend.lagom.scaladsl.persistence.slick.SlickPersistenceComponents
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.server.{ LagomApplicationContext, LagomServer }
import com.softwaremill.macwire.wire
import de.upb.cs.uc4.authentication.api.AuthenticationService
import de.upb.cs.uc4.authentication.impl.actor.{ AuthenticationBehaviour, AuthenticationState }
import de.upb.cs.uc4.authentication.impl.commands.DeleteAuthentication
import de.upb.cs.uc4.authentication.impl.readside.{ AuthenticationDatabase, AuthenticationEventProcessor }
import de.upb.cs.uc4.authentication.model.JsonUsername
import de.upb.cs.uc4.shared.client.Hashing
import de.upb.cs.uc4.shared.client.kafka.EncryptionContainer
import de.upb.cs.uc4.shared.server.kafka.KafkaEncryptionComponent
import de.upb.cs.uc4.shared.server.messages.Confirmation
import de.upb.cs.uc4.shared.server.UC4Application
import de.upb.cs.uc4.user.api.UserService
import org.slf4j.{ Logger, LoggerFactory }
import play.api.db.HikariCPComponents

import scala.concurrent.Future
import scala.concurrent.duration._

abstract class AuthenticationApplication(context: LagomApplicationContext)
  extends UC4Application(context)
  with SlickPersistenceComponents
  with JdbcPersistenceComponents
  with HikariCPComponents
  with LagomKafkaComponents
  with KafkaEncryptionComponent {

  private final val log: Logger = LoggerFactory.getLogger("Shared")

  private implicit val timeout: Timeout = Timeout(5.seconds)

  // Create ReadSide
  lazy val database: AuthenticationDatabase = wire[AuthenticationDatabase]
  lazy val processor: AuthenticationEventProcessor = wire[AuthenticationEventProcessor]
  readSide.register(processor)

  // Bind the service that this server provides
  override lazy val lagomServer: LagomServer = serverFor[AuthenticationService](wire[AuthenticationServiceImpl])

  // Register the JSON serializer registry
  override lazy val jsonSerializerRegistry: JsonSerializerRegistry = AuthenticationSerializerRegistry

  // Initialize the sharding of the Aggregate. The following starts the aggregate Behavior under
  // a given sharding entity typeKey.
  clusterSharding.init(
    Entity(AuthenticationState.typeKey)(
      entityContext => AuthenticationBehaviour.create(entityContext)
    )
  )

  lazy val userService: UserService = serviceClient.implement[UserService]

  userService
    .userDeletionTopic()
    .subscribe
    .atLeastOnce(
      Flow.fromFunction[EncryptionContainer, Future[Done]] { container =>
        try {
          val jsonUsername = kafkaEncryptionUtility.decrypt[JsonUsername](container)
          clusterSharding.entityRefFor(AuthenticationState.typeKey, Hashing.sha256(jsonUsername.username))
            .ask[Confirmation](replyTo => DeleteAuthentication(replyTo)).map(_ => Done)
        }
        catch {
          case throwable: Throwable =>
            log.error("AuthenticationService received invalid topic message: {}", throwable.toString)
            Future.successful(Done)
        }
      }
        .mapAsync(8)(done => done)
    )
}

object AuthenticationApplication {
  val offset: String = "UC4Authentication"
}

