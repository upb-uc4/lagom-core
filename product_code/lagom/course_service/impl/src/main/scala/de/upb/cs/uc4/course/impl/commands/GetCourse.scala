package de.upb.cs.uc4.course.impl.commands

import akka.actor.typed.ActorRef
import de.upb.cs.uc4.course.model.Course

case class GetCourse(replyTo: ActorRef[Option[Course]]) extends CourseCommand
