package de.upb.cs.uc4.authentication.impl.events

import play.api.libs.json.{ Format, Json }

case class OnDelete(username: String) extends AuthenticationEvent

object OnDelete {
  implicit val format: Format[OnDelete] = Json.format
}
