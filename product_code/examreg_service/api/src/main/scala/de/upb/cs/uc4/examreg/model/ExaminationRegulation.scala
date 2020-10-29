package de.upb.cs.uc4.examreg.model

import play.api.libs.json.{ Format, Json }

case class ExaminationRegulation(name: String, active: Boolean, modules: Seq[Module])

object ExaminationRegulation {
  implicit val format: Format[ExaminationRegulation] = Json.format
}