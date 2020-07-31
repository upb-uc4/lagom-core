package de.upb.cs.uc4.matriculation.model

import play.api.libs.json.{Format, Json}

case class ImmatriculationStatus(fieldOfStudy: String, intervals: Seq[Interval]) {
  def trim: ImmatriculationStatus =
    copy(fieldOfStudy = fieldOfStudy.trim)
}

object ImmatriculationStatus {
  implicit val format: Format[ImmatriculationStatus] = Json.format
}