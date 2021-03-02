package de.upb.cs.uc4.exam.impl.actor

import de.upb.cs.uc4.exam.model.Exam
import play.api.libs.json.{ Format, Json }

case class ExamsWrapper(exams: Seq[Exam])

object ExamsWrapper {
  implicit val format: Format[ExamsWrapper] = Json.format
}