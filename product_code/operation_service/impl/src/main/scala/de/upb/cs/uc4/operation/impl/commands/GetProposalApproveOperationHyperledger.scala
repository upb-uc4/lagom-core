package de.upb.cs.uc4.operation.impl.commands

import akka.actor.typed.ActorRef
import akka.pattern.StatusReply
import de.upb.cs.uc4.hyperledger.impl.commands.HyperledgerReadCommand
import de.upb.cs.uc4.operation.impl.actor.RawUnsignedProposal

case class GetProposalApproveOperationHyperledger(certificate: String, operationId: String, replyTo: ActorRef[StatusReply[RawUnsignedProposal]]) extends HyperledgerReadCommand[RawUnsignedProposal]