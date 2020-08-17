package de.upb.cs.uc4.authentication.impl.events

import de.upb.cs.uc4.authentication.model.AuthenticationUser
import play.api.libs.json.{ Format, Json }

case class OnSet(user: AuthenticationUser) extends AuthenticationEvent

object OnSet {
  implicit val format: Format[OnSet] = Json.format
}
