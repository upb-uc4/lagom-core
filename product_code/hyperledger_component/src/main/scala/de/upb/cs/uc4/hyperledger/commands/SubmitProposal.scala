package de.upb.cs.uc4.hyperledger.commands

import akka.actor.typed.ActorRef
import com.google.protobuf.ByteString
import de.upb.cs.uc4.shared.client.SignedTransactionProposal
import de.upb.cs.uc4.shared.server.messages.Confirmation

case class SubmitProposal(unsignedProposal: Array[Byte], signature: Array[Byte], replyTo: ActorRef[Confirmation]) extends HyperledgerWriteCommand

object SubmitProposal {
  def apply(proposal: SignedTransactionProposal, replyTo: ActorRef[Confirmation]) =
    new SubmitProposal(proposal.unsignedProposalAsByteArray, proposal.signatureAsByteArray, replyTo)
}
