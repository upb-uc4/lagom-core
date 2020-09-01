package de.upb.cs.uc4.course.impl.commands

import akka.actor.typed.ActorRef
import de.upb.cs.uc4.shared.server.messages.Confirmation

case class DeleteCourse(id: String, replyTo: ActorRef[Confirmation]) extends CourseCommand
