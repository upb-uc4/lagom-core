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
import de.upb.cs.uc4.certificate.impl.readside.CertificateEventProcessor
import de.upb.cs.uc4.hyperledger.HyperledgerAdminParts
import de.upb.cs.uc4.hyperledger.utilities.traits.{ EnrollmentManagerTrait, RegistrationManagerTrait }
import de.upb.cs.uc4.hyperledger.utilities.{ EnrollmentManager, RegistrationManager }
import de.upb.cs.uc4.shared.client.exceptions.UC4Exception
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
  with HikariCPComponents
  with HyperledgerAdminParts {

  lazy val enrollmentManager: EnrollmentManagerTrait = EnrollmentManager
  lazy val registrationManager: RegistrationManagerTrait = RegistrationManager
  lazy val processor: CertificateEventProcessor = wire[CertificateEventProcessor]

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

  try {
    EnrollmentManager.enroll(caURL, tlsCert, walletPath, adminUsername, adminPassword, organisationId, channel, chaincode, networkDescriptionPath)
  }
  catch {
    case e: Throwable => throw UC4Exception.InternalServerError("Enrollment Error", e.getMessage, e)
  }

  implicit val timeout: Timeout = Timeout(15.seconds)

  lazy val userService: UserService = serviceClient.implement[UserService]

  userService
    .userCreationTopic()
    .subscribe
    .atLeastOnce(
      Flow.fromFunction[Usernames, Future[Done]](usernames =>
        registerUser(usernames.username, usernames.enrollmentId)).mapAsync(8)(done => done)
    )

  private def registerUser(username: String, enrollmentId: String): Future[Done] = {
    try {
      val secret = registrationManager.register(caURL, tlsCert, enrollmentId, adminUsername, walletPath, organisationName)
      clusterSharding.entityRefFor(CertificateState.typeKey, username)
        .ask[Confirmation](replyTo => RegisterUser(enrollmentId, secret, replyTo)).map(_ => Done)
    }
    catch {
      case e: Throwable => throw UC4Exception.InternalServerError("Registration Error", e.getMessage, e)
    }
  }
}

object CertificateApplication {
  /** Functions as offset for the database */
  val offset: String = "UniversityCredits4Certificate"
}
