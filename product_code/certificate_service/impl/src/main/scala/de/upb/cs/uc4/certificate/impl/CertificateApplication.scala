package de.upb.cs.uc4.certificate.impl

import akka.Done
import akka.cluster.sharding.typed.scaladsl.Entity
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import com.lightbend.lagom.scaladsl.persistence.jdbc.JdbcPersistenceComponents
import com.lightbend.lagom.scaladsl.persistence.slick.SlickPersistenceComponents
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.server.{ LagomApplicationContext, LagomServer }
import com.softwaremill.macwire.wire
import de.upb.cs.uc4.certificate.api.CertificateService
import de.upb.cs.uc4.certificate.impl.actor.{ CertificateBehaviour, CertificateState }
import de.upb.cs.uc4.certificate.impl.commands.RegisterUser
import de.upb.cs.uc4.shared.server.UC4Application
import de.upb.cs.uc4.shared.server.messages.Confirmation
import de.upb.cs.uc4.user.api.UserService
import de.upb.cs.uc4.user.model.Usernames
import play.api.db.HikariCPComponents

import scala.concurrent.Future
import scala.concurrent.duration._

abstract class CertificateApplication(context: LagomApplicationContext)
  extends UC4Application(context)
    with SlickPersistenceComponents
    with JdbcPersistenceComponents
    with HikariCPComponents {

  // Bind the service that this server provides
  override lazy val lagomServer: LagomServer = serverFor[CertificateService](wire[CertificateServiceImpl])

  // Register the JSON serializer registry
  override lazy val jsonSerializerRegistry: JsonSerializerRegistry = CertificateSerializerRegistry

  // Initialize the sharding of the Aggregate. The following starts the aggregate Behavior under
  // a given sharding entity typeKey.
  clusterSharding.init(
    Entity(CertificateState.typeKey)(
      entityContext => CertificateBehaviour.create(entityContext)
    )
  )

  implicit val timeout: Timeout = Timeout(15.seconds)

  lazy val userService: UserService = serviceClient.implement[UserService]

  userService
    .userCreationTopic()
    .subscribe
    .atLeastOnce(
      Flow.fromFunction[Usernames, Future[Done]](usernames =>
        clusterSharding.entityRefFor(CertificateState.typeKey, usernames.username)
          .ask[Confirmation](replyTo => RegisterUser(usernames.enrollmentId, replyTo)).map(_ => Done)).mapAsync(8)(done => done)
    )
}

object CertificateApplication {
  /** Functions as offset for the database */
  val offset: String = "UniversityCredits4Certificate"
}
