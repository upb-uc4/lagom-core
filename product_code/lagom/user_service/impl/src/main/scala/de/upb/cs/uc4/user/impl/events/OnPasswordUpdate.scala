package de.upb.cs.uc4.user.impl.events

import de.upb.cs.uc4.user.model.user.AuthenticationUser
import play.api.libs.json.{Format, Json}

case class OnPasswordUpdate(authenticationUser: AuthenticationUser) extends UserEvent

object OnPasswordUpdate {
  implicit val format: Format[OnPasswordUpdate] = Json.format
}