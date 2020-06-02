package de.upb.cs.uc4.impl.commands

import akka.actor.typed.ActorRef
import de.upb.cs.uc4.model.Course

case class GetCourse(replyTo: ActorRef[Course]) extends CourseCommand
