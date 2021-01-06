package de.upb.cs.uc4.user.model

import de.upb.cs.uc4.user.model.Role.Role
import play.api.libs.json.{Format, Json}

case class Usernames(username: String, enrollmentId: String, role: Role)

object Usernames {
  implicit val format: Format[Usernames] = Json.format
}