package de.upb.cs.uc4.impl.events

import de.upb.cs.uc4.model.Course
import play.api.libs.json.{Format, Json}

case class OnCourseCreate(course : Course) extends CourseEvent

object OnCourseCreate{
  implicit val format: Format[OnCourseCreate] = Json.format
}
