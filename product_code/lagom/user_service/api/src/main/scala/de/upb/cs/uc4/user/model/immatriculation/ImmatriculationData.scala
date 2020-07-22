package de.upb.cs.uc4.user.model.immatriculation

import play.api.libs.json.{Format, Json}

case class ImmatriculationData(matriculationId: String,
                               firstName: String,
                               lastName: String,
                               birthDate: String,
                               immatriculationStatus: Seq[ImmatriculationStatus])

object ImmatriculationData {
  implicit val format: Format[ImmatriculationData] = Json.format
}