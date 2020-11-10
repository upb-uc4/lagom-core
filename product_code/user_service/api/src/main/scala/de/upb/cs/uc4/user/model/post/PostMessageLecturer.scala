package de.upb.cs.uc4.user.model.post

import de.upb.cs.uc4.authentication.model.AuthenticationUser
import de.upb.cs.uc4.shared.client.exceptions.SimpleError
import de.upb.cs.uc4.user.model.user.{ Lecturer, User }
import play.api.libs.json.{ Format, Json }

import scala.concurrent.{ ExecutionContext, Future }

case class PostMessageLecturer(authUser: AuthenticationUser, user: Lecturer) extends PostMessageUser {

  def copyPostMessageUser(
      authUser: AuthenticationUser = this.authUser,
      user: User = this.user
  ): PostMessageLecturer = copy(authUser, user.asInstanceOf[Lecturer])

  override def validate(implicit ec: ExecutionContext): Future[Seq[SimpleError]] = {
    user.validate.flatMap { lecturerErrors =>
      var errors = lecturerErrors.map {
        simpleError =>
          SimpleError("lecturer." + simpleError.name, simpleError.reason)
      }

      super.validate.map { superErrors =>
        errors ++= superErrors

        //Filter username errors if authUsername and username are not equal + return error that usernames are not equal
        if (authUser.username != user.username) {
          errors.filter(simpleError => !simpleError.name.contains("username"))
          errors :+= SimpleError("authUser.username", "Username in authUser must match username in lecturer")
          errors :+= SimpleError("lecturer.username", "Username in lecturer must match username in authUser")
        }

        errors
      }
    }

  }

  override def trim: PostMessageLecturer =
    super.trim.asInstanceOf[PostMessageLecturer].copy(
      user = user.trim
    )

  override def clean: PostMessageLecturer =
    super.clean.asInstanceOf[PostMessageLecturer].copy(
      user = user.clean
    )
}

object PostMessageLecturer {
  implicit val format: Format[PostMessageLecturer] = Json.format
}
