package de.upb.cs.uc4.course.model
import play.api.libs.json.{ Format, Json }

object CourseLanguage extends Enumeration {
  type CourseLanguage = Value
  val German: CourseLanguage = Value("German")
  val English: CourseLanguage = Value("English")

  implicit val format: Format[CourseLanguage] = Json.formatEnum(this)

  def All: Seq[CourseLanguage] = values.toSeq
}
