package de.upb.cs.uc4.user.model

import play.api.libs.json.{ Format, Json }

case class Usernames(username: String, enrollmentId: String)

object Usernames {
  implicit val format: Format[Usernames] = Json.format
}