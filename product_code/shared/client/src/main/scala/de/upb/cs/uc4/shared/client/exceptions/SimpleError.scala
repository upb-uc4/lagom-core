package de.upb.cs.uc4.shared.client.exceptions

import play.api.libs.json.{ Format, Json }

case class SimpleError(name: String, reason: String)

object SimpleError {
  implicit val format: Format[SimpleError] = Json.format
}