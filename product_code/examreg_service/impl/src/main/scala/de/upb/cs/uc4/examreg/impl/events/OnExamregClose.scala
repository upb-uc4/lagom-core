package de.upb.cs.uc4.examreg.impl.events

import de.upb.cs.uc4.examreg.model.ExaminationRegulation
import play.api.libs.json.{ Format, Json }

case class OnExamregClose(examreg: ExaminationRegulation) extends ExamregEvent

object OnExamregClose {
  implicit val format: Format[OnExamregClose] = Json.format
}
