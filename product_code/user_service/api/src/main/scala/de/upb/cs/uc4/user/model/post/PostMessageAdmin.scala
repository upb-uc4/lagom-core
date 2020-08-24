package de.upb.cs.uc4.user.model.post

import de.upb.cs.uc4.authentication.model.AuthenticationUser
import de.upb.cs.uc4.user.model.user.Admin
import play.api.libs.json.{ Format, Json }

case class PostMessageAdmin(authUser: AuthenticationUser, admin: Admin) extends PostMessageUser {

  def copyPostMessageUser(
      authUser: AuthenticationUser = this.authUser
  ): PostMessageAdmin = copy(authUser, admin)

  override def trim: PostMessageAdmin =
    super.trim.asInstanceOf[PostMessageAdmin].copy(
      admin = admin.trim
    )

  override def clean: PostMessageAdmin =
    super.clean.asInstanceOf[PostMessageAdmin].copy(
      admin = admin.clean
    )
}

object PostMessageAdmin {
  implicit val format: Format[PostMessageAdmin] = Json.format
}
