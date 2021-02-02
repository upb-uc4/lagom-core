package de.upb.cs.uc4.admission.impl.commands

import akka.actor.typed.ActorRef
import akka.pattern.StatusReply
import de.upb.cs.uc4.admission.model.ExamAdmission
import de.upb.cs.uc4.hyperledger.ProposalWrapper
import de.upb.cs.uc4.hyperledger.commands.HyperledgerProposalCommand

case class GetProposalForAddExamAdmission(certificate: String, examAdmission: ExamAdmission, replyTo: ActorRef[StatusReply[ProposalWrapper]]) extends HyperledgerProposalCommand

