package de.upb.cs.uc4.user.model

import de.upb.cs.uc4.user.model.user.{ Admin, Lecturer, Student }
import play.api.libs.json.{ Format, Json }

case class GetAllUsersResponse(students: Seq[Student], lecturers: Seq[Lecturer], admins: Seq[Admin])

object GetAllUsersResponse {
  implicit val format: Format[GetAllUsersResponse] = Json.format
}
