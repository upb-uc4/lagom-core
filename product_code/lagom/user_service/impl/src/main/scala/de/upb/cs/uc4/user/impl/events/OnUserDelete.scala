package de.upb.cs.uc4.user.impl.events

import de.upb.cs.uc4.user.model.user.User
import play.api.libs.json.{Format, Json}

case class OnUserDelete(user: User) extends UserEvent

object OnUserDelete {
  implicit val format: Format[OnUserDelete] = Json.format
}