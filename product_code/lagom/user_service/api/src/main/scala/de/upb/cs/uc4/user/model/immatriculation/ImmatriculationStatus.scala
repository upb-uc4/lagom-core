package de.upb.cs.uc4.user.model.immatriculation

import play.api.libs.json.{Format, Json}

case class ImmatriculationStatus(fieldOfStudy: String, intervals: Seq[Interval])

object ImmatriculationStatus {
  implicit val format: Format[ImmatriculationStatus] = Json.format
}