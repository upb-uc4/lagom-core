package de.upb.cs.uc4.admission.impl.commands

import akka.actor.typed.ActorRef
import akka.pattern.StatusReply
import de.upb.cs.uc4.admission.model.AbstractAdmission
import de.upb.cs.uc4.hyperledger.impl.ProposalWrapper
import de.upb.cs.uc4.hyperledger.impl.commands.HyperledgerProposalCommand

case class GetProposalForAddAdmission(certificate: String, admission: AbstractAdmission, replyTo: ActorRef[StatusReply[ProposalWrapper]]) extends HyperledgerProposalCommand
