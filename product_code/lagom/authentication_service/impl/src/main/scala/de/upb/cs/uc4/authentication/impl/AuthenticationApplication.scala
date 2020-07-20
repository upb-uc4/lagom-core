package de.upb.cs.uc4.authentication.impl

import akka.Done
import akka.cluster.sharding.typed.scaladsl.Entity
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.persistence.jdbc.JdbcPersistenceComponents
import com.lightbend.lagom.scaladsl.persistence.slick.SlickPersistenceComponents
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.server.{LagomApplication, LagomApplicationContext, LagomServer}
import com.softwaremill.macwire.wire
import de.upb.cs.uc4.authentication.api.AuthenticationService
import de.upb.cs.uc4.authentication.impl.actor.{AuthenticationBehaviour, AuthenticationState}
import de.upb.cs.uc4.authentication.impl.commands.{DeleteAuthentication, SetAuthentication}
import de.upb.cs.uc4.authentication.impl.readside.{AuthenticationDatabase, AuthenticationEventProcessor}
import de.upb.cs.uc4.shared.server.Hashing
import de.upb.cs.uc4.shared.server.messages.Confirmation
import de.upb.cs.uc4.user.api.UserService
import de.upb.cs.uc4.user.model.JsonUsername
import de.upb.cs.uc4.user.model.user.AuthenticationUser
import play.api.db.HikariCPComponents
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.mvc.EssentialFilter
import play.filters.cors.CORSComponents

import scala.concurrent.Future
import scala.concurrent.duration._

abstract class AuthenticationApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with SlickPersistenceComponents
    with JdbcPersistenceComponents
    with HikariCPComponents
    with LagomKafkaComponents
    with CORSComponents
    with AhcWSComponents {

  private implicit val timeout: Timeout = Timeout(5.seconds)

  // Create ReadSide
  lazy val database: AuthenticationDatabase = wire[AuthenticationDatabase]
  lazy val processor: AuthenticationEventProcessor = wire[AuthenticationEventProcessor]

  // Set HttpFilter to the default CorsFilter
  override val httpFilters: Seq[EssentialFilter] = Seq(corsFilter)

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
    .userAuthenticationTopic()
    .subscribe
    .atLeastOnce(
      Flow.fromFunction[AuthenticationUser, Future[Done]](user => {
        clusterSharding.entityRefFor(AuthenticationState.typeKey, Hashing.sha256(user.username))
          .ask[Confirmation](replyTo => SetAuthentication(user, replyTo)).map(_ => Done)
      }).mapAsync(8)(done => done)
    )

  userService
    .userDeletedTopic()
    .subscribe
    .atLeastOnce(
      Flow.fromFunction[JsonUsername, Future[Done]](json =>
        clusterSharding.entityRefFor(AuthenticationState.typeKey, Hashing.sha256(json.username))
          .ask[Confirmation](replyTo => DeleteAuthentication(replyTo)).map(_ => Done)
      ).mapAsync(8)(done => done)
    )
}

object AuthenticationApplication {
  val offset: String = "UC4Authentication"
}


