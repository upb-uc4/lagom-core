package de.upb.cs.uc4.user.model.post

import de.upb.cs.uc4.authentication.model.AuthenticationUser
import de.upb.cs.uc4.shared.client.exceptions.SimpleError
import de.upb.cs.uc4.user.model.user.Admin
import play.api.libs.json.{ Format, Json }

import scala.concurrent.{ ExecutionContext, Future }

case class PostMessageAdmin(authUser: AuthenticationUser, governmentId: String, admin: Admin) extends PostMessageUser {

  def copyPostMessageUser(
      authUser: AuthenticationUser = this.authUser,
      governmentId: String = this.governmentId
  ): PostMessageAdmin = copy(authUser, governmentId, admin)

  override def validate(implicit ec: ExecutionContext): Future[Seq[SimpleError]] = {
    admin.validate.flatMap { adminErrors =>
      var errors = adminErrors.map {
        simpleError =>
          SimpleError("admin." + simpleError.name, simpleError.reason)
      }

      super.validate.map { superErrors =>
        errors ++= superErrors

        //Filter username errors if authUsername and username are not equal + return error that usernames are not equal
        if (authUser.username != admin.username) {
          errors.filter(simpleError => !simpleError.name.contains("username"))
          errors :+= SimpleError("authUser.username", "Username in authUser must match username in admin")
          errors :+= SimpleError("admin.username", "Username in admin must match username in authUser")
        }

        errors
      }
    }

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
