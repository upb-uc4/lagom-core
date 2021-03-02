package de.upb.cs.uc4.exam.impl.commands

import akka.actor.typed.ActorRef
import akka.pattern.StatusReply
import de.upb.cs.uc4.exam.model.Exam
import de.upb.cs.uc4.hyperledger.impl.ProposalWrapper
import de.upb.cs.uc4.hyperledger.impl.commands.HyperledgerProposalCommand

case class GetProposalAddExam(certificate: String, exam: Exam, replyTo: ActorRef[StatusReply[ProposalWrapper]]) extends HyperledgerProposalCommand