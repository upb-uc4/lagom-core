package de.upb.cs.uc4.course.impl.commands

import akka.actor.typed.ActorRef
import de.upb.cs.uc4.course.model.Course
import de.upb.cs.uc4.shared.messages.Confirmation

case class CreateCourse(course: Course, replyTo: ActorRef[Confirmation]) extends CourseCommand
