package de.upb.cs.uc4.impl.commands

import akka.actor.typed.ActorRef
import de.upb.cs.uc4.model.Course
import de.upb.cs.uc4.shared.messages.Confirmation

case class UpdateCourse(course: Course, replyTo: ActorRef[Confirmation]) extends CourseCommand