package de.upb.cs.uc4.hyperledger

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import akka.pattern.StatusReply
import de.upb.cs.uc4.hyperledger.HyperledgerUtils.ExceptionUtils
import de.upb.cs.uc4.hyperledger.commands._
import de.upb.cs.uc4.hyperledger.connections.traits.ConnectionTrait
import de.upb.cs.uc4.hyperledger.utilities.EnrollmentManager
import de.upb.cs.uc4.shared.client.exceptions.UC4Exception
import de.upb.cs.uc4.shared.server.messages.Accepted
import org.slf4j.{ Logger, LoggerFactory }

trait HyperledgerDefaultActorFactory[Connection <: ConnectionTrait] extends HyperledgerAdminParts {

  protected final val log: Logger = LoggerFactory.getLogger(getClass)

  /** The companion object */
  val companionObject: HyperledgerActorObject

  /** Creates an actor */
  def create(): Behavior[HyperledgerBaseCommand] = Behaviors.setup { _ =>
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

    def start(): Behavior[HyperledgerBaseCommand] =
      Behaviors.receive {
        case (_, internCommand) =>
          internCommand match {

            case Activation() =>
              log.info(s"Initialised connection with chaincode version ${connection.getChaincodeVersion}.")
              Behaviors.same

            case Shutdown() =>
              if (connection != null) {
                connection.close()
              }
              Behaviors.same

            case cmd: HyperledgerInternCommand[_] =>
              if (enrolled) {
                try {
                  cmd match {
                    case SubmitProposal(proposal, signature, replyTo) =>
                      connection.submitSignedProposal(proposal, signature)
                      replyTo ! StatusReply.success(Accepted.default)
                    case command: HyperledgerCommand[_] => applyCommand(connection, command)
                  }
                }
                catch {
                  case ex: Exception =>
                    cmd.replyTo ! StatusReply.error(ex.toUC4Exception)
                }
                Behaviors.same
              }
              else {
                cmd.replyTo ! StatusReply.error(UC4Exception.InternalServerError("Enrollment Error", "No connection to the chain"))
                Behaviors.stopped
              }
          }
      }

    start()
  }

  /** Creates the connection to the chaincode */
  protected def createConnection: Connection

  /** Gets called every time when the actor receives a command.
    * [[SubmitProposal]] does not need to be handled here.
    * Errors which this method will thrown will be handled accordingly
    * if the command implements [[HyperledgerReadCommand]] or the
    * [[HyperledgerWriteCommand]].
    *
    * @param connection the current active connection
    * @param command which should get executed
    */
  protected def applyCommand(connection: Connection, command: HyperledgerCommand[_]): Unit
}

trait HyperledgerActorObject {

  /** The EntityTypeKey of this actor */
  val typeKey: EntityTypeKey[HyperledgerBaseCommand]

  /** The reference to the entity */
  val entityId: String
}
