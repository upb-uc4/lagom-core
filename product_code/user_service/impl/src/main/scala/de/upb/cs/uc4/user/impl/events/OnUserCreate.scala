package de.upb.cs.uc4.user.impl.events

import de.upb.cs.uc4.user.model.user.User
import play.api.libs.json.{Format, Json}

case class OnUserCreate(user: User, governmentId: String) extends UserEvent

object OnUserCreate {
  implicit val format: Format[OnUserCreate] = Json.format
}