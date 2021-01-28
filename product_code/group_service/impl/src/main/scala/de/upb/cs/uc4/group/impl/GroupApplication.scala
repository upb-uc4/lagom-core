package de.upb.cs.uc4.group.impl

import akka.Done
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.server.{ LagomApplicationContext, LagomServer }
import com.softwaremill.macwire.wire
import de.upb.cs.uc4.certificate.api.CertificateService
import de.upb.cs.uc4.certificate.model.EnrollmentUser
import de.upb.cs.uc4.group.api.GroupService
import de.upb.cs.uc4.group.impl.actor.GroupBehaviour
import de.upb.cs.uc4.group.impl.commands.AddToGroup
import de.upb.cs.uc4.hyperledger.HyperledgerComponent
import de.upb.cs.uc4.shared.client.kafka.EncryptionContainer
import de.upb.cs.uc4.shared.server.UC4Application
import de.upb.cs.uc4.shared.server.kafka.KafkaEncryptionComponent
import de.upb.cs.uc4.shared.server.messages.Confirmation
import org.slf4j.{ Logger, LoggerFactory }

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

abstract class GroupApplication(context: LagomApplicationContext)
  extends UC4Application(context)
  with HyperledgerComponent
  with KafkaEncryptionComponent {

  protected final val log: Logger = LoggerFactory.getLogger(getClass)

  override def createActorFactory: GroupBehaviour = wire[GroupBehaviour]

  // Bind CertificateService
  lazy val certificateService: CertificateService = serviceClient.implement[CertificateService]

  // Register the JSON serializer registry
  override lazy val jsonSerializerRegistry: JsonSerializerRegistry = GroupSerializerRegistry

  // Bind the service that this server provides
  override lazy val lagomServer: LagomServer = serverFor[GroupService](wire[GroupServiceImpl])

  implicit val timeout: Timeout = Timeout(config.getInt("uc4.timeouts.hyperledger").milliseconds)

  // Register System and Registration-System to a group
  addToGroup(config.getString("uc4.hyperledger.systemEnrollmentId"), config.getString("uc4.hyperledger.systemGroup"))
    .recover {
      case throwable: Throwable =>
        log.error("GroupService can't add system: {}", throwable.toString)
    }

  addToGroup(config.getString("uc4.hyperledger.registrationSystemEnrollmentId"), config.getString("uc4.hyperledger.systemGroup"))
    .recover {
      case throwable: Throwable =>
        log.error("GroupService can't add registration-system: {}", throwable.toString)
    }

  certificateService
    .userEnrollmentTopic()
    .subscribe
    .atLeastOnce(
      Flow.fromFunction[EncryptionContainer, Future[Done]] { container =>
        try {
          val user = kafkaEncryptionUtility.decrypt[EnrollmentUser](container)
          addToGroup(user.enrollmentId, user.role)
            .recover {
              case throwable: Throwable =>
                log.error("GroupService received invalid topic message: {}", throwable.toString)
                Done
            }

        }
        catch {
          case throwable: Throwable =>
            log.error("GroupService received invalid topic message: {}", throwable.toString)
            Future.successful(Done)
        }
      }
        .mapAsync(8)(done => done)
    )

  private def addToGroup(enrollmentId: String, role: String): Future[Done] = {
    clusterSharding.entityRefFor(GroupBehaviour.typeKey, GroupBehaviour.entityId)
      .askWithStatus[Confirmation](replyTo => AddToGroup(enrollmentId, role, replyTo))
      .map(_ => Done)
  }
}
