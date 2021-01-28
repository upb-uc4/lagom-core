package de.upb.cs.uc4.operation.model

import play.api.libs.json.{ Format, Json }

case class JsonRejectMessage(rejectMessage: String)

object JsonRejectMessage {
  implicit val format: Format[JsonRejectMessage] = Json.format
}