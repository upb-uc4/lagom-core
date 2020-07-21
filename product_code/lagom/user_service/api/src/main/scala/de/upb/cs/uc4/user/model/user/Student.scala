package de.upb.cs.uc4.user.model.user

import de.upb.cs.uc4.shared.client.SimpleError
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

  override def validate: Seq[SimpleError] = {
    val fos = List("Computer Science","Philosophy","Media Sciences", "Economics", "Mathematics", "Physics", "Chemistry",
      "Education", "Sports Science", "Japanology", "Spanish Culture", "Pedagogy", "Business Informatics", "Linguistics")

    var errors = List[SimpleError]()
    errors ++= super.validate
    if(!(matriculationId forall Character.isDigit) || !(matriculationId.toInt > 0)) {
      errors :+= SimpleError("matriculationId", "Student ID invalid.")
    }
    if(!(semesterCount > 0)) {
      errors :+= SimpleError("semesterCount", "Semester count must be a positive integer.")
    }
    if(!fieldsOfStudy.forall(fos.contains)) {
      errors :+= SimpleError("fieldsOfStudy", "Fields of Study must be one of [..].")
    }
    errors
  }
}

object Student {
  implicit val format: Format[Student] = Json.format
}
