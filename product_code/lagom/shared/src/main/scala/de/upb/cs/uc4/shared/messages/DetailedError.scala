package de.upb.cs.uc4.shared.messages

import play.api.libs.json.{Format, Json}

case class DetailedError(name: String, detail: String)

object DetailedError {
  implicit val format: Format[DetailedError] = Json.format
}