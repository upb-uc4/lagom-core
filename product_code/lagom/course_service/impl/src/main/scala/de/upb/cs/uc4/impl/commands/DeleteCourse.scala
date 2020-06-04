package de.upb.cs.uc4.impl.commands

import akka.actor.typed.ActorRef
import de.upb.cs.uc4.shared.messages.Confirmation

case class DeleteCourse(id: Long, replyTo: ActorRef[Confirmation]) extends CourseCommand