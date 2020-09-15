package de.upb.cs.uc4.user.impl.events

import play.api.libs.json.{ Format, Json }

case class OnImageSet(username: String, image: Array[Byte]) extends UserEvent

object OnImageSet {
  implicit val format: Format[OnImageSet] = Json.format
}