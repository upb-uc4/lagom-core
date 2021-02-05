package de.upb.cs.uc4.operation.impl

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
import de.upb.cs.uc4.hyperledger.impl.HyperledgerComponent
import de.upb.cs.uc4.operation.api.OperationService
import de.upb.cs.uc4.operation.impl.actor.{ OperationDatabaseBehaviour, OperationHyperledgerBehaviour, OperationState }
import de.upb.cs.uc4.operation.impl.commands.ClearWatchlist
import de.upb.cs.uc4.operation.impl.readside.OperationEventProcessor
import de.upb.cs.uc4.shared.client.JsonUsername
import de.upb.cs.uc4.shared.client.kafka.EncryptionContainer
import de.upb.cs.uc4.shared.server.UC4Application
import de.upb.cs.uc4.shared.server.kafka.KafkaEncryptionComponent
import de.upb.cs.uc4.shared.server.messages.Confirmation
import de.upb.cs.uc4.user.api.UserService
import de.upb.cs.uc4.user.model.JsonUserData
import org.slf4j.{ Logger, LoggerFactory }
import play.api.db.HikariCPComponents

import scala.concurrent.Future
import scala.concurrent.duration._

abstract class OperationApplication(context: LagomApplicationContext)
  extends UC4Application(context)
  with SlickPersistenceComponents
  with JdbcPersistenceComponents
  with HikariCPComponents
  with HyperledgerComponent
  with KafkaEncryptionComponent {

  private final val log: Logger = LoggerFactory.getLogger(classOf[OperationApplication])
  lazy val processor: OperationEventProcessor = wire[OperationEventProcessor]
  readSide.register(processor)

  override def createHyperledgerActor: OperationHyperledgerBehaviour = wire[OperationHyperledgerBehaviour]

  //Bind CertificateService and UserService (for Topic)
  lazy val certificateService: CertificateService = serviceClient.implement[CertificateService]
  lazy val userService: UserService = serviceClient.implement[UserService]

  // Register the JSON serializer registry
  override lazy val jsonSerializerRegistry: JsonSerializerRegistry = OperationSerializerRegistry

  // Bind the service that this server provides
  override lazy val lagomServer: LagomServer = serverFor[OperationService](wire[OperationServiceImpl])

  // Initialize the sharding of the Aggregate. The following starts the aggregate Behavior under
  // a given sharding entity typeKey.
  clusterSharding.init(
    Entity(OperationState.typeKey)(
      entityContext => OperationDatabaseBehaviour.create(entityContext)
    )
  )

  implicit val timeout: Timeout = Timeout(config.getInt("uc4.timeouts.database").milliseconds)

  userService
    .userDeletionTopicMinimal()
    .subscribe
    .atLeastOnce(
      Flow.fromFunction[EncryptionContainer, Future[Done]] { container =>
        try {
          val jsonUsername = kafkaEncryptionUtility.decrypt[JsonUsername](container)
          clusterSharding.entityRefFor(OperationState.typeKey, jsonUsername.username)
            .ask[Confirmation](replyTo => ClearWatchlist(jsonUsername.username, replyTo)).map(_ => Done)
        }
        catch {
          case throwable: Throwable =>
            log.error("OperationService received invalid topic message: {}", throwable.toString)
            Future.successful(Done)
        }
      }
        .mapAsync(8)(done => done)
    )
}

object OperationApplication {
  /** Functions as offset for the database */
  val offset: String = "UniversityCredits4Operation"
}
