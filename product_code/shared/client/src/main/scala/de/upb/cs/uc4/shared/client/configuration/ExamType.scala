package de.upb.cs.uc4.shared.client.configuration

import play.api.libs.json.{ Format, Json }

object ExamType extends Enumeration {
  type ExamType = Value
  val WrittenExam: ExamType = Value("Written Exam")

  implicit val format: Format[ExamType] = Json.formatEnum(this)

  def All: Seq[ExamType] = values.toSeq
}
