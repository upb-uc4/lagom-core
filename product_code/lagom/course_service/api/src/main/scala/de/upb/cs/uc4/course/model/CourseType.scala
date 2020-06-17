package de.upb.cs.uc4.course.model

import play.api.libs.json.{Format, Json}

object CourseType extends Enumeration {
  type CourseType = Value
  val Lecture = Value("Lecture")
  val Seminar = Value("Seminar")
  val ProjectGroup = Value("Project Group")
  //Todo no spaces in Enumeration (ApiDoc: Project Group)


  implicit val format: Format[CourseType] = Json.formatEnum(this)

  def All: Seq[CourseType] = values.toSeq
}
