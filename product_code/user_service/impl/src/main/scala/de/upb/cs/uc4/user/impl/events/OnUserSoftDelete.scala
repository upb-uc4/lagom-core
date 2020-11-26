package de.upb.cs.uc4.user.impl.events

import de.upb.cs.uc4.user.model.user.User
import play.api.libs.json.{ Format, Json }

case class OnUserSoftDelete(user: User) extends UserEvent

object OnUserSoftDelete {
  implicit val format: Format[OnUserSoftDelete] = Json.format
}