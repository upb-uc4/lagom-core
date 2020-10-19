package de.upb.cs.uc4.hyperledger

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import de.upb.cs.uc4.hyperledger.HyperledgerUtils.ExceptionUtils
import de.upb.cs.uc4.hyperledger.commands.{ HyperledgerCommand, HyperledgerReadCommand, HyperledgerWriteCommand, Shutdown }
import de.upb.cs.uc4.hyperledger.connections.traits.ConnectionTrait
import de.upb.cs.uc4.hyperledger.utilities.EnrollmentManager
import de.upb.cs.uc4.shared.client.exceptions.UC4Exception
import de.upb.cs.uc4.shared.server.messages.RejectedWithError

import scala.util.Failure

trait HyperledgerDefaultActorFactory[Connection <: ConnectionTrait] extends HyperledgerAdminParts {

  /** The companion object */
  val companionObject: HyperledgerActorObject

  /** Creates an actor */
  def create(): Behavior[HyperledgerCommand] = Behaviors.setup { _ =>
    try {
      EnrollmentManager.enroll(caURL, tlsCert, walletPath, adminUsername, adminPassword, organisationId, channel, chaincode, networkDescriptionPath)
    }
    catch {
      case ex: Throwable => throw UC4Exception.InternalServerError("Enrollment Error in Actor", ex.getMessage, ex)
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
