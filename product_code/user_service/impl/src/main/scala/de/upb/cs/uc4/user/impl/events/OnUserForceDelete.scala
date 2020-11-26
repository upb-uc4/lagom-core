package de.upb.cs.uc4.user.impl.events

import de.upb.cs.uc4.user.model.user.User
import play.api.libs.json.{ Format, Json }

case class OnUserForceDelete(user: User) extends UserEvent

object OnUserForceDelete {
  implicit val format: Format[OnUserForceDelete] = Json.format
}