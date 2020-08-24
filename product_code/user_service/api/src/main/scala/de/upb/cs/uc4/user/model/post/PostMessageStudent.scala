package de.upb.cs.uc4.user.model.post

import de.upb.cs.uc4.authentication.model.AuthenticationUser
import de.upb.cs.uc4.user.model.user.Student
import play.api.libs.json.{ Format, Json }

case class PostMessageStudent(authUser: AuthenticationUser, student: Student) extends PostMessageUser {

  def copyPostMessageUser(
      authUser: AuthenticationUser = this.authUser
  ): PostMessageStudent = copy(authUser, student)

  override def trim: PostMessageStudent =
    super.trim.asInstanceOf[PostMessageStudent].copy(
      student = student.trim
    )

  override def clean: PostMessageStudent =
    super.clean.asInstanceOf[PostMessageStudent].copy(
      student = student.clean
    )
}

object PostMessageStudent {
  implicit val format: Format[PostMessageStudent] = Json.format
}

