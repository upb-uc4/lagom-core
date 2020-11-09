package de.upb.cs.uc4.user.model

import de.upb.cs.uc4.user.model.Role.Role
import play.api.libs.json.{ Format, Json }

case class JsonUserData(username: String, role: Role)

object JsonUserData {
  implicit val format: Format[JsonUserData] = Json.format
}