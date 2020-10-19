package de.upb.cs.uc4.configuration.model

import play.api.libs.json.{ Format, Json }

case class JsonSemester(semester: String)

object JsonSemester {
  implicit val format: Format[JsonSemester] = Json.format
}
