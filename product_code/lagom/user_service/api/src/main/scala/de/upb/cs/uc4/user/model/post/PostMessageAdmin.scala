package de.upb.cs.uc4.user.model.post

import de.upb.cs.uc4.authentication.model.AuthenticationUser
import de.upb.cs.uc4.user.model.user.Admin
import play.api.libs.json.{ Format, Json }

case class PostMessageAdmin(authUser: AuthenticationUser, admin: Admin)

object PostMessageAdmin {
  implicit val format: Format[PostMessageAdmin] = Json.format
}
