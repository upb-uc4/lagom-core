package de.upb.cs.uc4.user.impl.events

import de.upb.cs.uc4.user.model.user.{AuthenticationUser, User}
import play.api.libs.json.{Format, Json}

case class OnUserCreate(user: User, authenticationUser: AuthenticationUser) extends UserEvent

object OnUserCreate {
  implicit val format: Format[OnUserCreate] = Json.format
}