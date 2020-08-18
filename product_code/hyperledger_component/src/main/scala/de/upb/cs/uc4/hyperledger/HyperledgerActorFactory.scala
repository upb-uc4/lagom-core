package de.upb.cs.uc4.hyperledger

import java.nio.file.{ Path, Paths }

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import com.typesafe.config.Config
import de.upb.cs.uc4.hyperledger.HyperledgerUtils.ExceptionUtils
import de.upb.cs.uc4.hyperledger.commands.{ HyperledgerCommand, HyperledgerReadCommand, HyperledgerWriteCommand, Shutdown }
import de.upb.cs.uc4.hyperledger.connections.traits.ConnectionTrait
import de.upb.cs.uc4.hyperledger.utilities.EnrollmentManager
import de.upb.cs.uc4.shared.server.messages.RejectedWithError

import scala.util.Failure

trait HyperledgerActorFactory[Connection <: ConnectionTrait] {

  /** The configuration of the service */
  val config: Config

  /** The companion object */
  val companionObject: HyperledgerActorObject

  protected val walletPath: Path = retrievePath("uc4.hyperledger.wallet", "/hyperledger_assets/wallet/")
  protected val networkDescriptionPath: Path = retrievePath("uc4.hyperledger.networkConfig", "/hyperledger_assets/connection_profile.yaml")
  protected val tlsCert: Path = retrievePath("uc4.hyperledger.tlsCert", "")

  protected val username: String = retrieveString("uc4.hyperledger.username")
  protected val password: String = retrieveString("uc4.hyperledger.password")
  protected val organisationId: String = retrieveString("uc4.hyperledger.organisationId")

  protected val channel: String = retrieveString("uc4.hyperledger.channel")
  protected val chaincode: String = retrieveString("uc4.hyperledger.chaincode")
  protected val caURL: String = retrieveString("uc4.hyperledger.caURL")

  /** Creates an actor */
  def create(): Behavior[HyperledgerCommand] = Behaviors.setup { _ =>
    EnrollmentManager.enroll(caURL, tlsCert, walletPath, username, password, organisationId)

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
                  val customException = ex.toCustomException
                  cmd match {
                    case write: HyperledgerWriteCommand =>
                      write.replyTo ! RejectedWithError(customException.getErrorCode.http, customException.getPossibleErrorResponse)
                    case read: HyperledgerReadCommand[_] =>
                      read.replyTo ! Failure(customException)
                  }
              }
              Behaviors.same
          }
      }

    start()
  }

  /** Retrieves the path from the key out of the configuration.
    *
    * @param key in the configuration
    * @param fallback the path as string if the key does not exist
    * @return the retrieved path
    */
  protected def retrievePath(key: String, fallback: String): Path = {
    if (config.hasPath(key)) {
      Paths.get(config.getString(key))
    }
    else {
      Paths.get(getClass.getResource(fallback).toURI)
    }
  }

  /** Retrieves a string from the key out of the configuration.
    *
    * @param key in the configuration
    * @param fallback used if the key does not exist
    * @return the retrieved string
    */
  protected def retrieveString(key: String, fallback: String = ""): String = {
    if (config.hasPath(key)) {
      config.getString(key)
    }
    else {
      fallback
    }
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
  protected def applyCommand(connection: Connection, command: HyperledgerCommand)
}

trait HyperledgerActorObject {

  /** The EntityTypeKey of this actor */
  val typeKey: EntityTypeKey[HyperledgerCommand]

  /** The reference to the entity */
  val entityId: String
}
