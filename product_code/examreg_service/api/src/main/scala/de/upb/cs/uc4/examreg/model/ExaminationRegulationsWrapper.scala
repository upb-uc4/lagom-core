package de.upb.cs.uc4.examreg.model

import play.api.libs.json.{ Format, Json }

case class ExaminationRegulationsWrapper(examinationRegulations: Seq[ExaminationRegulation])

object ExaminationRegulationsWrapper {
  implicit val format: Format[ExaminationRegulationsWrapper] = Json.format
}
