package de.upb.cs.uc4.operation.impl.actor

import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import com.typesafe.config.Config
import de.upb.cs.uc4.hyperledger.commands.{HyperledgerBaseCommand, HyperledgerCommand, HyperledgerReadCommand, HyperledgerWriteCommand}
import de.upb.cs.uc4.hyperledger.connections.cases.ConnectionOperation
import de.upb.cs.uc4.hyperledger.connections.traits.ConnectionOperationsTrait
import de.upb.cs.uc4.hyperledger.{HyperledgerActorObject, HyperledgerDefaultActorFactory}

class OperationBehaviour(val config: Config) extends HyperledgerDefaultActorFactory[ConnectionOperationsTrait] {

  /** Creates the connection to the chaincode */
  override protected def createConnection: ConnectionOperationsTrait =
    ConnectionOperation(adminUsername, channel, chaincode, walletPath, networkDescriptionPath)

  /** Gets called every time when the actor receives a command
    * Errors which this method will thrown will be handled accordingly
    * if the command implements [[HyperledgerReadCommand]] or the
    * [[HyperledgerWriteCommand]].
    *
    * @param connection the current active connection
    * @param command which should get executed
    */
  override protected def applyCommand(connection: ConnectionOperationsTrait, command: HyperledgerCommand[_]): Unit = command match {
    case _ =>
  }

  /** The companion object */
  override val companionObject: HyperledgerActorObject = OperationBehaviour
}

object OperationBehaviour extends HyperledgerActorObject {
  /** The EntityTypeKey of this actor */
  override val typeKey: EntityTypeKey[HyperledgerBaseCommand] = EntityTypeKey[HyperledgerBaseCommand]("uc4operation")
  /** The reference to the entity */
  override val entityId: String = "operation"
}
