package de.upb.cs.uc4.user.impl.events

import play.api.libs.json.{ Format, Json }

case class OnImageDelete(username: String) extends UserEvent

object OnImageDelete {
  implicit val format: Format[OnImageDelete] = Json.format
}
