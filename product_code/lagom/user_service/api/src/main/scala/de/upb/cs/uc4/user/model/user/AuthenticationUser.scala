package de.upb.cs.uc4.user.model.user

import de.upb.cs.uc4.user.model.Role.Role
import play.api.libs.json.{Format, Json}

case class AuthenticationUser(username: String, password: String, role: Role)

object AuthenticationUser {
  implicit val format: Format[AuthenticationUser] = Json.format
}
