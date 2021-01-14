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
import de.upb.cs.uc4.certificate.impl.commands.{ ForceDeleteCertificateUser, RegisterUser, SoftDeleteCertificateUser }
import de.upb.cs.uc4.certificate.impl.readside.{ CertificateDatabase, CertificateEventProcessor }
import de.upb.cs.uc4.hyperledger.HyperledgerAdminParts
import de.upb.cs.uc4.hyperledger.HyperledgerUtils.ExceptionUtils
import de.upb.cs.uc4.hyperledger.utilities.traits.{ EnrollmentManagerTrait, RegistrationManagerTrait }
import de.upb.cs.uc4.hyperledger.utilities.{ EnrollmentManager, RegistrationManager }
import de.upb.cs.uc4.shared.client.kafka.EncryptionContainer
import de.upb.cs.uc4.shared.server.UC4Application
import de.upb.cs.uc4.shared.server.kafka.KafkaEncryptionComponent
import de.upb.cs.uc4.shared.server.messages.Confirmation
import de.upb.cs.uc4.user.api.UserService
import de.upb.cs.uc4.user.model.{ JsonUserData, Usernames }
import org.slf4j.{ Logger, LoggerFactory }
import play.api.db.HikariCPComponents

import scala.concurrent.Future
import scala.concurrent.duration._

abstract class CertificateApplication(context: LagomApplicationContext)
  extends UC4Application(context)
  with SlickPersistenceComponents
  with JdbcPersistenceComponents
  with HikariCPComponents
  with HyperledgerAdminParts
  with KafkaEncryptionComponent {

  protected final val log: Logger = LoggerFactory.getLogger(classOf[CertificateApplication])

  lazy val enrollmentManager: EnrollmentManagerTrait = EnrollmentManager
  lazy val registrationManager: RegistrationManagerTrait = RegistrationManager

  lazy val database: CertificateDatabase = wire[CertificateDatabase]
  lazy val processor: CertificateEventProcessor = wire[CertificateEventProcessor]
  readSide.register(processor)

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
    enrollmentManager.enroll(caURL, tlsCert, walletPath, adminUsername, adminPassword, organisationId, channel, chaincode, networkDescriptionPath)
  }
  catch {
    case e: Throwable => throw e.toUC4Exception
  }

  implicit val timeout: Timeout = Timeout(config.getInt("uc4.timeouts.database").milliseconds)

  lazy val userService: UserService = serviceClient.implement[UserService]

  userService
    .userCreationTopic()
    .subscribe
    .atLeastOnce(
      Flow.fromFunction[EncryptionContainer, Future[Done]](container => try {
        val usernames = kafkaEncryptionUtility.decrypt[Usernames](container)
        registerUser(usernames.username, usernames.enrollmentId, usernames.role.toString)
      }
      catch {
        case throwable: Throwable =>
          log.error("CertificateService received invalid topic message: {}", throwable.toString)
          Future.successful(Done)
      }).mapAsync(8)(done => done)
    )

  userService
    .userDeletionTopicPrecise()
    .subscribe
    .atLeastOnce(
      Flow.fromFunction[EncryptionContainer, Future[Done]] { container =>
        try {
          val jsonUserData = kafkaEncryptionUtility.decrypt[JsonUserData](container)
          if (jsonUserData.forceDelete) {
            clusterSharding.entityRefFor(CertificateState.typeKey, jsonUserData.username)
              .ask[Confirmation](replyTo => ForceDeleteCertificateUser(jsonUserData.username, jsonUserData.role, replyTo)).map(_ => Done)
          }
          else {
            clusterSharding.entityRefFor(CertificateState.typeKey, jsonUserData.username)
              .ask[Confirmation](replyTo => SoftDeleteCertificateUser(jsonUserData.username, jsonUserData.role, replyTo)).map(_ => Done)
          }

        }
        catch {
          case throwable: Throwable =>
            log.error("CertificateService received invalid topic message: {}", throwable.toString)
            Future.successful(Done)
        }
      }
        .mapAsync(8)(done => done)
    )

  private def registerUser(username: String, enrollmentId: String, role: String): Future[Done] = {
    try {
      val secret = registrationManager.register(caURL, tlsCert, enrollmentId, adminUsername, walletPath, organisationName)
      clusterSharding.entityRefFor(CertificateState.typeKey, username)
        .ask[Confirmation](replyTo => RegisterUser(username, enrollmentId, secret, role, replyTo)).map(_ => Done)
    }
    catch {
      case e: Throwable =>
        log.error("Registration failed", e.toUC4Exception)
        Future.successful(Done)
    }
  }
}

object CertificateApplication {
  /** Functions as offset for the database */
  val offset: String = "UniversityCredits4Certificate"
}
