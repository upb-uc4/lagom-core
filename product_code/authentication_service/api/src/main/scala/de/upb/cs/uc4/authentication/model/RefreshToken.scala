package de.upb.cs.uc4.authentication.model

import play.api.libs.json.{ Format, Json }

case class RefreshToken(login: String, username: String)

object RefreshToken {
  implicit val format: Format[RefreshToken] = Json.format
}