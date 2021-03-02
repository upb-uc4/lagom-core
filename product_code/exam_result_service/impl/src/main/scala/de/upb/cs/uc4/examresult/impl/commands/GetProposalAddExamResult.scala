package de.upb.cs.uc4.examresult.impl.commands

import akka.actor.typed.ActorRef
import akka.pattern.StatusReply
import de.upb.cs.uc4.examresult.model.ExamResult
import de.upb.cs.uc4.hyperledger.impl.ProposalWrapper
import de.upb.cs.uc4.hyperledger.impl.commands.HyperledgerProposalCommand

case class GetProposalAddExamResult(certificate: String, examResult: ExamResult, replyTo: ActorRef[StatusReply[ProposalWrapper]]) extends HyperledgerProposalCommand