package de.upb.cs.uc4.examreg.impl.events

import de.upb.cs.uc4.examreg.model.ExaminationRegulation
import play.api.libs.json.{ Format, Json }

case class OnExamregCreate(examreg: ExaminationRegulation) extends ExamregEvent

object OnExamregCreate {
  implicit val format: Format[OnExamregCreate] = Json.format
}
