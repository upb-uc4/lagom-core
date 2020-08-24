package de.upb.cs.uc4.user.model.post

import de.upb.cs.uc4.authentication.model.AuthenticationUser
import de.upb.cs.uc4.user.model.user.Lecturer
import play.api.libs.json.{ Format, Json }

case class PostMessageLecturer(authUser: AuthenticationUser, lecturer: Lecturer) extends PostMessageUser {

  def copyPostMessageUser(
      authUser: AuthenticationUser = this.authUser
  ): PostMessageLecturer = copy(authUser, lecturer)

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
