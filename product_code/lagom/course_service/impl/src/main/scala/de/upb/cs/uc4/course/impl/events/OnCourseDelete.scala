package de.upb.cs.uc4.course.impl.events

import play.api.libs.json.{Format, Json}

case class OnCourseDelete(id: Long) extends CourseEvent

object OnCourseDelete{
  implicit val format: Format[OnCourseDelete] = Json.format
}
