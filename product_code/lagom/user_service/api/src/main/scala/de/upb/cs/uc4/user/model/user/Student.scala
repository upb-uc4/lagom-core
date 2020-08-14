package de.upb.cs.uc4.user.model.user

import de.upb.cs.uc4.shared.client.exceptions.SimpleError
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
                   phoneNumber: String,
                   birthDate: String,
                   immatriculationStatus: String,
                   matriculationId: String,
                   semesterCount: Int,
                   fieldsOfStudy: List[String]) extends User {

  def copyUser(username: String = this.username,
               role: Role = this.role,
               address: Address = this.address,
               firstName: String = this.firstName,
               lastName: String = this.lastName,
               picture: String = this.picture,
               email: String = this.email,
               phoneNumber: String = this.phoneNumber,
               birthDate: String = this.birthDate): Student =
    copy(username, role, address, firstName, lastName, picture, email, phoneNumber, birthDate)

  override def trim: Student =
    super.trim.asInstanceOf[Student].copy(
      immatriculationStatus = immatriculationStatus.trim, matriculationId = matriculationId.trim)

  override def toPublic: Student =
    super.toPublic.asInstanceOf[Student].copy(immatriculationStatus = "", matriculationId = "")

  override def clean: Student = super.clean.asInstanceOf[Student]

  /** @inheritdoc */
  override def validate: Seq[SimpleError] = {
    val fos = List("Computer Science", "Philosophy", "Media Sciences", "Economics", "Mathematics", "Physics", "Chemistry",
      "Education", "Sports Science", "Japanology", "Spanish Culture", "Pedagogy", "Business Informatics", "Linguistics")

    var errors = super.validate.asInstanceOf[List[SimpleError]]
    if (matriculationId.isEmpty) {
      errors :+= SimpleError("matriculationId", "Matriculation ID must not be empty.")
    } else {
      if (!(matriculationId forall Character.isDigit) || !(matriculationId.toInt > 0) || !(matriculationId.toInt < 10000000)) {
        errors :+= SimpleError("matriculationId", "Matriculation ID must be an integer between 0000001 and 9999999.")
      } else {
        if (matriculationId.length != 7) {
          errors :+= SimpleError("matriculationId", "Matriculation ID must be a string of length 7.")
        }
      }
    }
    if (!(semesterCount > 0)) {
      errors :+= SimpleError("semesterCount", "Semester count must be a positive integer.")
    }
    if (!(fieldsOfStudy forall fos.contains)) {
      errors :+= SimpleError("fieldsOfStudy", "Fields of Study must be one of [..].")
    }
    errors
  }


  /**
   * Compares the object against the user parameter to find out if fields, which should only be changed by users with elevated privileges, are different.
   * Returns a list of [[SimpleError]]
   *
   * @param user to be checked
   * @return Filled Sequence of [[SimpleError]]
   */
  override def checkEditableFields(user: User): Seq[SimpleError] = {
    if (!user.isInstanceOf[Student]) {
      throw new Exception("Tried to parse a non-Student as Student.")
    }
    val student = user.asInstanceOf[Student]

    var errors = List[SimpleError]()

    errors ++= super.checkEditableFields(user)

    if (immatriculationStatus != student.immatriculationStatus) {
      errors :+= SimpleError("immatriculationStatus", "Immatriculation status may not be manually changed.")
    }
    if (matriculationId != student.matriculationId) {
      errors :+= SimpleError("matriculationId", "Matriculation ID may not be manually changed.")
    }
    if (semesterCount != student.semesterCount) {
      errors :+= SimpleError("semesterCount", "Number of semesters may not be manually changed.")
    }
    if (fieldsOfStudy != student.fieldsOfStudy) {
      errors :+= SimpleError("fieldsOfStudy", "Fields of study may not be manually changed.")
    }
    errors
  }
}

object Student {
  implicit val format: Format[Student] = Json.format
}
