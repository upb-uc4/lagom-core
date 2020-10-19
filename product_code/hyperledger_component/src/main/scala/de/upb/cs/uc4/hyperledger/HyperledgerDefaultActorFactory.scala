package de.upb.cs.uc4.hyperledger

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import de.upb.cs.uc4.hyperledger.HyperledgerUtils.ExceptionUtils
import de.upb.cs.uc4.hyperledger.commands.{ HyperledgerCommand, HyperledgerReadCommand, HyperledgerWriteCommand, Shutdown }
import de.upb.cs.uc4.hyperledger.connections.traits.ConnectionTrait
import de.upb.cs.uc4.hyperledger.utilities.EnrollmentManager
import de.upb.cs.uc4.shared.client.exceptions.{ ErrorType, GenericError, UC4Exception }
import de.upb.cs.uc4.shared.server.messages.RejectedWithError
import org.slf4j.{ Logger, LoggerFactory }

import scala.util.Failure

trait HyperledgerDefaultActorFactory[Connection <: ConnectionTrait] extends HyperledgerAdminParts {

  protected final val log: Logger = LoggerFactory.getLogger(getClass)

  /** The companion object */
  val companionObject: HyperledgerActorObject

  /** Creates an actor */
  def create(): Behavior[HyperledgerCommand] = Behaviors.setup { _ =>
    val enrolled = try {
      EnrollmentManager.enroll(caURL, tlsCert, walletPath, adminUsername, adminPassword, organisationId, channel, chaincode, networkDescriptionPath)
      true
    }
    catch {
      case ex: Throwable =>
        log.error("Enrollment not possible in the creation", UC4Exception.InternalServerError("Enrollment Error in Actor", ex.getMessage, ex))
        false
    }
    lazy val connection = createConnection

    def start(): Behavior[HyperledgerCommand] =
      Behaviors.receive {
        case (_, cmd) =>
          cmd match {

            case Shutdown() =>
              if (connection != null) {
                connection.close()
              }
              Behaviors.same

            case cmd: HyperledgerCommand =>
              if (enrolled) {
                try {
                  applyCommand(connection, cmd)
                }
                catch {
                  case ex: Exception =>
                    val customException = ex.toUC4Exception
                    cmd match {
                      case write: HyperledgerWriteCommand =>
                        write.replyTo ! RejectedWithError(customException.errorCode.http, customException.possibleErrorResponse)
                      case read: HyperledgerReadCommand[_] =>
                        read.replyTo ! Failure(customException)
                    }
                }
              }
              else {
                cmd match {
                  case write: HyperledgerWriteCommand =>
                    write.replyTo ! RejectedWithError(500, GenericError(ErrorType.InternalServer, "No connection to the chain"))
                  case read: HyperledgerReadCommand[_] =>
                    read.replyTo ! Failure(UC4Exception.InternalServerError("Enrollment Error", "No connection to the chain"))
                }
              }
              Behaviors.same
          }
      }

    start()
  }

  /** Creates the connection to the chaincode */
  protected def createConnection: Connection

  /** Gets called every time when the actor receives a command.
    * Errors which this method will thrown will be handled accordingly
    * if the command implements [[HyperledgerReadCommand]] or the
    * [[HyperledgerWriteCommand]].
    *
    * @param connection the current active connection
    * @param command which should get executed
    */
  protected def applyCommand(connection: Connection, command: HyperledgerCommand): Unit
}

trait HyperledgerActorObject {

  /** The EntityTypeKey of this actor */
  val typeKey: EntityTypeKey[HyperledgerCommand]

  /** The reference to the entity */
  val entityId: String
}
