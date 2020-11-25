package de.upb.cs.uc4.examreg.impl.commands

import akka.actor.typed.ActorRef
import de.upb.cs.uc4.examreg.model.ExaminationRegulation

case class GetExamreg(replyTo: ActorRef[Option[ExaminationRegulation]]) extends ExamregCommand
