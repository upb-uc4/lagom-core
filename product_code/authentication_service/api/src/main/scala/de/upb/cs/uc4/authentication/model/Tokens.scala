package de.upb.cs.uc4.authentication.model

import play.api.libs.json.{ Format, Json }

case class Tokens(login: String, refresh: String)

object Tokens {
  implicit val format: Format[Tokens] = Json.format
}
