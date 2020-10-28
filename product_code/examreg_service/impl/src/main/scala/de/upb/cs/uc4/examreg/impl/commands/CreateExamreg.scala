package de.upb.cs.uc4.examreg.impl.commands

import akka.actor.typed.ActorRef
import de.upb.cs.uc4.examreg.model.ExaminationRegulation
import de.upb.cs.uc4.shared.server.messages.Confirmation

case class CreateExamreg(examreg: ExaminationRegulation, replyTo: ActorRef[Confirmation]) extends ExamregCommand
