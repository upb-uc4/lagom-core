package de.upb.cs.uc4.user.model.post

import de.upb.cs.uc4.user.model.user.{Admin, AuthenticationUser}
import play.api.libs.json.{Format, Json}

case class PostMessageAdmin(authUser: AuthenticationUser, admin: Admin)

object PostMessageAdmin {
  implicit val format: Format[PostMessageAdmin] = Json.format
}
