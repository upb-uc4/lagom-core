package de.upb.cs.uc4.examreg.impl.commands

import akka.actor.typed.ActorRef
import akka.pattern.StatusReply
import de.upb.cs.uc4.examreg.model.ExaminationRegulation
import de.upb.cs.uc4.hyperledger.commands.HyperledgerReadCommand

case class GetAllExamregsHyperledger(replyTo: ActorRef[StatusReply[Seq[ExaminationRegulation]]]) extends HyperledgerReadCommand[Seq[ExaminationRegulation]]
