package de.upb.cs.uc4.operation.impl.commands

import akka.actor.typed.ActorRef
import akka.pattern.StatusReply
import de.upb.cs.uc4.hyperledger.api.model.UnsignedProposal
import de.upb.cs.uc4.hyperledger.impl.commands.HyperledgerReadCommand

case class GetProposalRejectOperationHyperledger(certificate: String, operationId: String, rejectMessage: String, replyTo: ActorRef[StatusReply[UnsignedProposal]]) extends HyperledgerReadCommand[UnsignedProposal]