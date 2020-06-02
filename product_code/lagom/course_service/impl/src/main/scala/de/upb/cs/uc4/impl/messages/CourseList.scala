package de.upb.cs.uc4.impl.messages

import de.upb.cs.uc4.model.Course
import play.api.libs.json.{Format, Json}

case class CourseList(courses: List[Course]){

  def getCourses() : Seq[Course] = courses

}

object CourseList {
  implicit val format: Format[CourseList] = Json.format
}
