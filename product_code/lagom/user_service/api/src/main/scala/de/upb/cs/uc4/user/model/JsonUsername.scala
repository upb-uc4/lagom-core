package de.upb.cs.uc4.user.model

import play.api.libs.json.{Format, Json}

case class JsonUsername(username: String)

object JsonUsername {
  implicit val format: Format[JsonUsername] = Json.format
}