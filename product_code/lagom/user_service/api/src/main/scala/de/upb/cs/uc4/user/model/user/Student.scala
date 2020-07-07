package de.upb.cs.uc4.user.model.user

import de.upb.cs.uc4.user.model.Address
import de.upb.cs.uc4.user.model.Role.Role
import play.api.libs.json.{Format, Json}

case class Student(username: String,
                   role: Role,
                   address: Address,
                   firstName: String,
                   lastName: String,
                   picture: String,
                   email: String,
                   birthDate: String,
                   immatriculationStatus: String,
                   matriculationId: String,
                   semesterCount: Int,
                   fieldsOfStudy: List[String]) extends User {

  def trim: Student = {
    copy(username.trim, role, address.trim, firstName.trim, lastName.trim,
      picture.trim, email.trim, birthDate.trim, immatriculationStatus.trim, matriculationId.trim)
  }
}

object Student {
  implicit val format: Format[Student] = Json.format
}
