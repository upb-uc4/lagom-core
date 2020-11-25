package de.upb.cs.uc4.user.model.user

import de.upb.cs.uc4.shared.client.Utils.SemesterUtils
import de.upb.cs.uc4.shared.client.exceptions.{ SimpleError, UC4Exception }
import de.upb.cs.uc4.user.model.Address
import de.upb.cs.uc4.user.model.Role.Role
import play.api.libs.json.{ Format, Json }

import scala.concurrent.{ ExecutionContext, Future }

case class Student(
    username: String,
    enrollmentIdSecret: String,
    role: Role,
    address: Address,
    firstName: String,
    lastName: String,
    email: String,
    phoneNumber: String,
    birthDate: String,
    latestImmatriculation: String,
    matriculationId: String
) extends User {

  def copyUser(
      username: String = this.username,
      enrollmentIdSecret: String = this.enrollmentIdSecret,
      role: Role = this.role,
      address: Address = this.address,
      firstName: String = this.firstName,
      lastName: String = this.lastName,
      email: String = this.email,
      phoneNumber: String = this.phoneNumber,
      birthDate: String = this.birthDate
  ): Student =
    copy(username, enrollmentIdSecret, role, address, firstName, lastName, email, phoneNumber, birthDate)

  override def trim: Student =
    super.trim.asInstanceOf[Student].copy(
      latestImmatriculation = latestImmatriculation.trim,
      matriculationId = matriculationId.trim
    )

  override def toPublic: Student =
    Student(this.username, "", this.role, Address.empty, this.firstName, this.lastName, this.email, this.phoneNumber, "", "", "")

  override def clean: Student = super.clean.asInstanceOf[Student]

  /** @inheritdoc */
  override def validate(implicit ec: ExecutionContext): Future[Seq[SimpleError]] = {

    super.validate.map { superErrors =>
      var errors = superErrors.toList
      if (latestImmatriculation != "") {
        errors :++= latestImmatriculation.validateSemester.map(error => SimpleError("latestImmatriculation", error.reason))
      }

      if (matriculationId.isEmpty) {
        errors :+= SimpleError("matriculationId", "Matriculation ID must not be empty.")
      }
      else {
        if (!(matriculationId forall Character.isDigit) || !(matriculationId.toInt > 0) || !(matriculationId.toInt < 10000000)) {
          errors :+= SimpleError("matriculationId", "Matriculation ID must be an integer between 0000001 and 9999999.")
        }
        else {
          if (matriculationId.length != 7) {
            errors :+= SimpleError("matriculationId", "Matriculation ID must be a string of length 7.")
          }
        }
      }
      errors
    }
  }

  /** Compares the object against the user parameter to find out if fields, which should only be changed by users with elevated privileges, are different.
    * Returns a list of [[SimpleError]]
    *
    * @param user to be checked
    * @return Filled Sequence of [[SimpleError]]
    */
  override def checkProtectedFields(user: User): Seq[SimpleError] = {
    checkIfStudent(user)

    val student = user.asInstanceOf[Student]
    var errors = super.checkProtectedFields(user).asInstanceOf[List[SimpleError]]

    if (matriculationId != student.matriculationId) {
      errors :+= SimpleError("matriculationId", "Matriculation ID may not be manually changed.")
    }
    errors
  }

  /** Compares the object against the user parameter to find out if fields, which cannot be changed, are different.
    * Returns a list of SimpleErrors[[SimpleError]]
    *
    * @param user to be checked
    * @return Filled Sequence of [[SimpleError]]
    */
  override def checkUneditableFields(user: User): Seq[SimpleError] = {
    checkIfStudent(user)

    val student = user.asInstanceOf[Student]
    var errors = super.checkUneditableFields(student).asInstanceOf[List[SimpleError]]

    if (latestImmatriculation != student.latestImmatriculation) {
      errors :+= SimpleError("latestImmatriculation", "Latest Immatriculation must not be changed.")
    }

    errors
  }

  /** Helper method to check if the user is a student */
  private def checkIfStudent(user: User): Unit = {
    if (!user.isInstanceOf[Student]) {
      throw UC4Exception.InternalServerError("Wrong Entity", s"Expected a student, got a ${user.role}")
    }
  }
}

object Student {
  implicit val format: Format[Student] = Json.format
}
