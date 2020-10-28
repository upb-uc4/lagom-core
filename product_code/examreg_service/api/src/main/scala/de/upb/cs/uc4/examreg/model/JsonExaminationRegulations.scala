package de.upb.cs.uc4.examreg.model

import play.api.libs.json.{ Format, Json }

case class JsonExaminationRegulations(examinationRegulations: Seq[ExaminationRegulation])

object JsonExaminationRegulations {
  implicit val format: Format[JsonExaminationRegulations] = Json.format
}
