package de.upb.cs.uc4.user.model

import de.upb.cs.uc4.user.model.Role.Role
import play.api.libs.json.{Format, Json}

case class JsonRole(role: Role)

object JsonRole {
  implicit val format: Format[JsonRole] = Json.format
}