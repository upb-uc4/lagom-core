package de.upb.cs.uc4.shared.client

import play.api.libs.json.{Format, Json}

case class JsonVersionNumber(versionNumber: String)

object JsonVersionNumber {
  implicit val format: Format[JsonVersionNumber] = Json.format
}