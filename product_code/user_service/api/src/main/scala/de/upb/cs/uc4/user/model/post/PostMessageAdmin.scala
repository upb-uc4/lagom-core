package de.upb.cs.uc4.user.model.post

import de.upb.cs.uc4.authentication.model.AuthenticationUser
import de.upb.cs.uc4.shared.client.exceptions.SimpleError
import de.upb.cs.uc4.user.model.Role
import de.upb.cs.uc4.user.model.user.Admin
import play.api.libs.json.{ Format, Json }

case class PostMessageAdmin(authUser: AuthenticationUser, admin: Admin) extends PostMessageUser {

  def copyPostMessageUser(
      authUser: AuthenticationUser = this.authUser
  ): PostMessageAdmin = copy(authUser, admin)

  override def validate: Seq[SimpleError] = {
    var errors: List[SimpleError] = admin.validate.map {
      simpleError =>
        SimpleError("admin." + simpleError.name, simpleError.reason)
    }.toList
    errors ++= super.validate

    if (authUser.username != admin.username) {
      errors.filter(simpleError => !simpleError.name.contains("username"))
      errors :+= SimpleError("authUser.username", "Username in authUser must match username in admin")
      errors :+= SimpleError("admin.username", "Username in admin must match username in authUser")
    }
    errors
  }

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
