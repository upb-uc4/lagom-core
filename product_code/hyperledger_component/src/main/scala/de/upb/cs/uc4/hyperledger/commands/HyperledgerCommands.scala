package de.upb.cs.uc4.hyperledger.commands

import akka.actor.typed.ActorRef
import akka.pattern.StatusReply
import de.upb.cs.uc4.shared.client.{ SignedProposal, SignedTransaction }
import de.upb.cs.uc4.shared.server.messages.Confirmation

/** The trait for the commands needed in the state
  * Every command is a case class containing the
  * necessary information to execute the command
  */
sealed trait HyperledgerBaseCommand extends HyperledgerCommandSerializable

final case class Shutdown() extends HyperledgerBaseCommand

final case class Activation() extends HyperledgerBaseCommand

sealed trait HyperledgerInternCommand[PayloadType] extends HyperledgerBaseCommand {
  val replyTo: ActorRef[StatusReply[PayloadType]]
}

final case class SubmitProposal(unsignedProposal: Array[Byte], signature: Array[Byte], replyTo: ActorRef[StatusReply[Array[Byte]]]) extends HyperledgerInternCommand[Array[Byte]]

object SubmitProposal {
  def apply(proposal: SignedProposal, replyTo: ActorRef[StatusReply[Array[Byte]]]) =
    new SubmitProposal(proposal.unsignedProposalAsByteArray, proposal.signatureAsByteArray, replyTo)
}

final case class SubmitTransaction(unsignedTransaction: Array[Byte], signature: Array[Byte], replyTo: ActorRef[StatusReply[Confirmation]]) extends HyperledgerInternCommand[Confirmation]

object SubmitTransaction {
  def apply(transaction: SignedTransaction, replyTo: ActorRef[StatusReply[Confirmation]]) =
    new SubmitTransaction(transaction.unsignedTransactionAsByteArray, transaction.signatureAsByteArray, replyTo)
}

final case class GetChaincodeVersion(replyTo: ActorRef[StatusReply[Confirmation]]) extends HyperledgerInternCommand[Confirmation]

trait HyperledgerCommand[PayloadType] extends HyperledgerInternCommand[PayloadType]

trait HyperledgerReadCommand[PayloadType] extends HyperledgerCommand[PayloadType]

trait HyperledgerWriteCommand extends HyperledgerCommand[Confirmation]

