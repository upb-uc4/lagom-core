package de.upb.cs.uc4.admission.impl.commands

import akka.actor.typed.ActorRef
import akka.pattern.StatusReply
import de.upb.cs.uc4.admission.model.DropAdmission
import de.upb.cs.uc4.hyperledger.impl.ProposalWrapper
import de.upb.cs.uc4.hyperledger.impl.commands.HyperledgerProposalCommand

case class GetProposalForDropAdmission(certificate: String, dropAdmission: DropAdmission, replyTo: ActorRef[StatusReply[ProposalWrapper]]) extends HyperledgerProposalCommand

