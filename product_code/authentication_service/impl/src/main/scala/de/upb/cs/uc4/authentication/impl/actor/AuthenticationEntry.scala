package de.upb.cs.uc4.authentication.impl.actor

import de.upb.cs.uc4.authentication.model.AuthenticationRole.AuthenticationRole
import play.api.libs.json.{ Format, Json }

case class AuthenticationEntry(username: String, salt: String, password: String, role: AuthenticationRole)

object AuthenticationEntry {
  implicit val format: Format[AuthenticationEntry] = Json.format
}