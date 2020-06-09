package de.upb.cs.uc4.course.impl.events

import de.upb.cs.uc4.course.model.Course
import play.api.libs.json.{Format, Json}

case class OnCourseUpdate(course: Course) extends CourseEvent

object OnCourseUpdate{
  implicit val format: Format[OnCourseUpdate] = Json.format
}