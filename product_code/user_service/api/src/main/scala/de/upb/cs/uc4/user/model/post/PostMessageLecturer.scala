package de.upb.cs.uc4.user.model.post

import de.upb.cs.uc4.authentication.model.AuthenticationUser
import de.upb.cs.uc4.shared.client.exceptions.SimpleError
import de.upb.cs.uc4.user.model.Role
import de.upb.cs.uc4.user.model.user.Lecturer
import play.api.libs.json.{ Format, Json }

case class PostMessageLecturer(authUser: AuthenticationUser, lecturer: Lecturer) extends PostMessageUser {

  def copyPostMessageUser(
      authUser: AuthenticationUser = this.authUser
  ): PostMessageLecturer = copy(authUser, lecturer)

  override def validate: Seq[SimpleError] = {
    var errors: List[SimpleError] = lecturer.validate.map {
      simpleError =>
        SimpleError("lecturer." + simpleError.name, simpleError.reason)
    }.toList
    errors ++= super.validate

    if (authUser.username != lecturer.username) {
      errors.filter(simpleError => !simpleError.name.contains("username"))
      errors :+= SimpleError("authUser.username", "Username in authUser must match username in lecturer")
      errors :+= SimpleError("lecturer.username", "Username in lecturer must match username in authUser")
    }
    errors
  }

  override def trim: PostMessageLecturer =
    super.trim.asInstanceOf[PostMessageLecturer].copy(
      lecturer = lecturer.trim
    )

  override def clean: PostMessageLecturer =
    super.clean.asInstanceOf[PostMessageLecturer].copy(
      lecturer = lecturer.clean
    )
}

object PostMessageLecturer {
  implicit val format: Format[PostMessageLecturer] = Json.format
}
