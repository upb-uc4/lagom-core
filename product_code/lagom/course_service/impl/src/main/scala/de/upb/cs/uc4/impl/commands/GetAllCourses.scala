package de.upb.cs.uc4.impl.commands

import akka.actor.typed.ActorRef
import de.upb.cs.uc4.model.Course

case class GetAllCourses(replyTo: ActorRef[Seq[Course]]) extends CourseCommand
