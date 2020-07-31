package de.upb.cs.uc4.user.model.post

import de.upb.cs.uc4.authentication.model.AuthenticationUser
import de.upb.cs.uc4.user.model.user.Lecturer
import play.api.libs.json.{Format, Json}

case class PostMessageLecturer(authUser: AuthenticationUser, lecturer: Lecturer)

object PostMessageLecturer {
  implicit val format: Format[PostMessageLecturer] = Json.format
}
