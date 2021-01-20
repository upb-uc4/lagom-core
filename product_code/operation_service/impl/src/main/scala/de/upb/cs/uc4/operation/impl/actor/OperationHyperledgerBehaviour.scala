package de.upb.cs.uc4.operation.impl.actor

import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import akka.pattern.StatusReply
import com.typesafe.config.Config
import de.upb.cs.uc4.hyperledger.api.model.UnsignedProposal
import de.upb.cs.uc4.hyperledger.api.model.operation.OperationData
import de.upb.cs.uc4.hyperledger.connections.cases.ConnectionOperation
import de.upb.cs.uc4.hyperledger.connections.traits.ConnectionOperationTrait
import de.upb.cs.uc4.hyperledger.impl.commands.{ HyperledgerBaseCommand, HyperledgerCommand, HyperledgerReadCommand, HyperledgerWriteCommand, SubmitProposal, SubmitTransaction }
import de.upb.cs.uc4.hyperledger.impl.{ HyperledgerActor, HyperledgerActorObject }
import de.upb.cs.uc4.operation.impl.commands.{ GetOperationHyperledger, GetOperationsHyperledger, GetProposalRejectOperationHyperledger }
import de.upb.cs.uc4.shared.client.JsonUtility.FromJsonUtil
import de.upb.cs.uc4.shared.server.messages.Accepted

class OperationHyperledgerBehaviour(val config: Config) extends HyperledgerActor[ConnectionOperationTrait] {

  /** Creates the connection to the chaincode */
  override protected def createConnection: ConnectionOperationTrait =
    ConnectionOperation(adminUsername, channel, chaincode, walletPath, networkDescriptionPath)

  /** Gets called every time when the actor receives a command
    * Errors which this method will thrown will be handled accordingly
    * if the command implements [[HyperledgerReadCommand]] or the
    * [[HyperledgerWriteCommand]].
    *
    * @param connection the current active connection
    * @param command    which should get executed
    */
  override protected def applyCommand(connection: ConnectionOperationTrait, command: HyperledgerCommand[_]): Unit = command match {
    case SubmitProposal(proposal, signature, replyTo) =>
      replyTo ! StatusReply.success(connection.getUnsignedTransaction(proposal, signature))
    case SubmitTransaction(transaction, signature, replyTo) =>
      val jsonOperationData = connection.submitSignedTransaction(transaction, signature)
      val operationData = jsonOperationData.fromJson[OperationData]

      if (operationData.missingApprovals.isEmpty) {
        connection.executeTransaction(jsonOperationData)
      }

      replyTo ! StatusReply.success(Accepted.default)
    case GetOperationHyperledger(id, replyTo) =>
      replyTo ! StatusReply.success(connection.getOperations(List(id), "", "", "", "", List()).fromJson[OperationData])
    case GetOperationsHyperledger(operationIds, existingEnrollmentId, missingEnrollmentId, initiatorEnrollmentId, involvedEnrollmentId, states, replyTo) =>
      replyTo ! StatusReply.success(OperationDataList(
        connection.getOperations(operationIds.toList, existingEnrollmentId, missingEnrollmentId, initiatorEnrollmentId, involvedEnrollmentId, states.toList).fromJson[Seq[OperationData]]
      ))
    case GetProposalRejectOperationHyperledger(certificate, id, message, replyTo) =>
      replyTo ! StatusReply.success(UnsignedProposal(connection.getProposalRejectOperation(certificate = certificate, operationId = id, rejectMessage = message)))
  }

  /** The companion object */
  override val companionObject: HyperledgerActorObject = OperationHyperledgerBehaviour
}

object OperationHyperledgerBehaviour extends HyperledgerActorObject {
  /** The EntityTypeKey of this actor */
  override val typeKey: EntityTypeKey[HyperledgerBaseCommand] = EntityTypeKey[HyperledgerBaseCommand]("uc4operation")
}
