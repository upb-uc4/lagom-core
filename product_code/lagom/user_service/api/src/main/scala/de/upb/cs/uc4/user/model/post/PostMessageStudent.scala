package de.upb.cs.uc4.user.model.post

import de.upb.cs.uc4.authentication.model.AuthenticationUser
import de.upb.cs.uc4.user.model.user.Student
import play.api.libs.json.{Format, Json}

case class PostMessageStudent(authUser: AuthenticationUser, student: Student)

object PostMessageStudent {
  implicit val format: Format[PostMessageStudent] = Json.format
}
