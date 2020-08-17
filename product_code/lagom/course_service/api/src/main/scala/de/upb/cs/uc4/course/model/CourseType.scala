package de.upb.cs.uc4.course.model

import play.api.libs.json.{ Format, Json }

object CourseType extends Enumeration {
  type CourseType = Value
  val Lecture: CourseType = Value("Lecture")
  val Seminar: CourseType = Value("Seminar")
  val ProjectGroup: CourseType = Value("Project Group")

  implicit val format: Format[CourseType] = Json.formatEnum(this)

  def All: Seq[CourseType] = values.toSeq
}
