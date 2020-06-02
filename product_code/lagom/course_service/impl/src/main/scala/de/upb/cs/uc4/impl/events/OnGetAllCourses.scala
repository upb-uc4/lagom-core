package de.upb.cs.uc4.impl.events

import play.api.libs.json.{Format, Json}


case class OnGetAllCourses(thisIsNotUsed :String) extends CourseEvent

object OnGetAllCourses{
  implicit val format: Format[OnGetAllCourses] = Json.format
}