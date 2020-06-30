package de.upb.cs.uc4.user.impl.events

import de.upb.cs.uc4.user.impl.actor.User
import play.api.libs.json.{Format, Json}

case class OnUserUpdate(user: User) extends UserEvent

object OnUserUpdate {
  implicit val format: Format[OnUserUpdate] = Json.format
}