package de.upb.cs.uc4.operation.impl.actor

import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import akka.pattern.StatusReply
import com.typesafe.config.Config
import de.upb.cs.uc4.hyperledger.HyperledgerUtils.JsonUtil._
import de.upb.cs.uc4.hyperledger.commands.{ HyperledgerBaseCommand, HyperledgerCommand, HyperledgerReadCommand, HyperledgerWriteCommand }
import de.upb.cs.uc4.hyperledger.connections.cases.ConnectionOperation
import de.upb.cs.uc4.hyperledger.connections.traits.ConnectionOperationsTrait
import de.upb.cs.uc4.hyperledger.{ HyperledgerActorObject, HyperledgerDefaultActorFactory }
import de.upb.cs.uc4.operation.impl.commands.{ GetOperationHyperledger, GetOperationsHyperledger, RejectOperationHyperledger }
import de.upb.cs.uc4.shared.client.operation.OperationData
import de.upb.cs.uc4.shared.server.messages.Accepted

class OperationHyperledgerBehaviour(val config: Config) extends HyperledgerDefaultActorFactory[ConnectionOperationsTrait] {

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
    case GetOperationHyperledger(id, replyTo) =>
      replyTo ! StatusReply.success(connection.getOperationData(id).fromJson[OperationData])
    case GetOperationsHyperledger(existingEnrollmentId, missingEnrollmentId, initiatorEnrollmentId, state, replyTo) =>
      replyTo ! StatusReply.success(OperationDataList(connection.getOperations(existingEnrollmentId, missingEnrollmentId, initiatorEnrollmentId, state).fromJson[Seq[OperationData]]))
    case RejectOperationHyperledger(id, message, replyTo) =>
      connection.rejectTransaction(id, message)
      replyTo ! StatusReply.success(Accepted.default)
  }

  /** The companion object */
  override val companionObject: HyperledgerActorObject = OperationHyperledgerBehaviour
}

object OperationHyperledgerBehaviour extends HyperledgerActorObject {
  /** The EntityTypeKey of this actor */
  override val typeKey: EntityTypeKey[HyperledgerBaseCommand] = EntityTypeKey[HyperledgerBaseCommand]("uc4operation")
}
