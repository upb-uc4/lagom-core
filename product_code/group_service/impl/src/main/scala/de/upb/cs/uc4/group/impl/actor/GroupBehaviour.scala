package de.upb.cs.uc4.group.impl.actor

import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import akka.pattern.StatusReply
import com.typesafe.config.Config
import de.upb.cs.uc4.group.impl.commands.SetGroup
import de.upb.cs.uc4.hyperledger.commands.{HyperledgerBaseCommand, HyperledgerCommand, HyperledgerReadCommand, HyperledgerWriteCommand}
import de.upb.cs.uc4.hyperledger.connections.cases.ConnectionGroup
import de.upb.cs.uc4.hyperledger.connections.traits.ConnectionGroupTrait
import de.upb.cs.uc4.hyperledger.{HyperledgerActorObject, HyperledgerDefaultActorFactory}
import de.upb.cs.uc4.shared.server.messages.Accepted

class GroupBehaviour(val config: Config) extends HyperledgerDefaultActorFactory[ConnectionGroupTrait] {

  /** Creates the connection to the chaincode */
  override protected def createConnection: ConnectionGroupTrait =
    ConnectionGroup(adminUsername, channel, chaincode, walletPath, networkDescriptionPath)

  /** Gets called every time when the actor receives a command
    * Errors which this method will thrown will be handled accordingly
    * if the command implements [[HyperledgerReadCommand]] or the
    * [[HyperledgerWriteCommand]].
    *
    * @param connection the current active connection
    * @param command which should get executed
    */
  override protected def applyCommand(connection: ConnectionGroupTrait, command: HyperledgerCommand[_]): Unit = command match {
    case SetGroup(enrollmentId, role, replyTo) =>
      replyTo ! StatusReply.success(Accepted(connection.addUserToGroup(enrollmentId, role)))
  }

  /** The companion object */
  override val companionObject: HyperledgerActorObject = GroupBehaviour
}

object GroupBehaviour extends HyperledgerActorObject {
  /** The EntityTypeKey of this actor */
  override val typeKey: EntityTypeKey[HyperledgerBaseCommand] = EntityTypeKey[HyperledgerBaseCommand]("uc4group")
  /** The reference to the entity */
  override val entityId: String = "group"
}
