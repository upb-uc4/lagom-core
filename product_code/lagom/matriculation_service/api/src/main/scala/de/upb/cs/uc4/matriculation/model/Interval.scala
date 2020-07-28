package de.upb.cs.uc4.matriculation.model

import play.api.libs.json.{Format, Json}

case class Interval(firstSemester: String, lastSemester: String)

object Interval {
  implicit val format: Format[Interval] = Json.format
}
