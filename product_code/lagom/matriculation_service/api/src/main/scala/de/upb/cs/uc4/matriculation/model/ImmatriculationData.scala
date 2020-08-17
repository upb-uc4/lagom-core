package de.upb.cs.uc4.matriculation.model

import play.api.libs.json.{ Format, Json }

case class ImmatriculationData(
    matriculationId: String,
    firstName: String,
    lastName: String,
    birthDate: String,
    matriculationStatus: Seq[SubjectMatriculation]
)

object ImmatriculationData {
  implicit val format: Format[ImmatriculationData] = Json.format
}