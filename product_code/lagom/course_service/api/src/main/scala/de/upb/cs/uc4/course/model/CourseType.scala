package de.upb.cs.uc4.course.model

import play.api.libs.json.{Format, Json}

object CourseType extends Enumeration {
  type CourseType = Value
  val Lecture, Seminar, ProjectGroup = Value
  //Todo no spaces in Enumeration (ApiDoc: Project Group)


  implicit val format: Format[CourseType] = Json.formatEnum(this)

  def All: Seq[CourseType] = values.toSeq
}
