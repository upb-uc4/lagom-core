package de.upb.cs.uc4.user.model

import de.upb.cs.uc4.user.model.Role.Role
import play.api.libs.json.{Format, Json}

case class User(username: String, password: String, role: Role)

object User {
  implicit val format: Format[User] = Json.format
}
