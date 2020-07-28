package de.upb.cs.uc4.user.model.post

import de.upb.cs.uc4.matriculation.model.ImmatriculationStatus
import de.upb.cs.uc4.user.model.user.{AuthenticationUser, Student}
import play.api.libs.json.{Format, Json}

case class PostMessageStudent(authUser: AuthenticationUser, student: Student, immatriculationStatus: ImmatriculationStatus)

object PostMessageStudent {
  implicit val format: Format[PostMessageStudent] = Json.format
}

