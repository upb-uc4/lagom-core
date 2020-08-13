package de.upb.cs.uc4.user.model.user

import de.upb.cs.uc4.shared.client.exceptions.SimpleError
import de.upb.cs.uc4.user.model.Address
import de.upb.cs.uc4.user.model.Role.Role
import play.api.libs.json.{Format, Json}
import de.upb.cs.uc4.shared.client.Utils.SemesterUtils

case class Student(username: String,
                   role: Role,
                   address: Address,
                   firstName: String,
                   lastName: String,
                   picture: String,
                   email: String,
                   phoneNumber: String,
                   birthDate: String,
                   latestImmatriculation: String,
                   matriculationId: String) extends User {

  def trim: Student = {
    copy(username.trim, role, address.trim, firstName.trim, lastName.trim,
      picture.trim, email.trim, phoneNumber.trim, birthDate.trim, latestImmatriculation.trim, matriculationId.trim)
  }

  def clean: Student = {
    trim.copy(email = email.toLowerCase, phoneNumber = phoneNumber.replaceAll("\\s+", ""))
  }

  def toPublic: Student = {
    copy(address = Address.empty, birthDate = "", latestImmatriculation = "", matriculationId = "")
  }

  /** @inheritdoc */
  override def validate: Seq[SimpleError] = {
    var errors = super.validate.asInstanceOf[List[SimpleError]]

    if(latestImmatriculation != ""){
      errors :++= latestImmatriculation.validateSemester.map(error => SimpleError("latestImmatriculation", error.reason))
    }

    if(matriculationId.isEmpty) {
      errors :+= SimpleError("matriculationId", "Matriculation ID must not be empty.")
    }else{
      if(!(matriculationId forall Character.isDigit) || !(matriculationId.toInt > 0) || !(matriculationId.toInt < 10000000)) {
        errors :+= SimpleError("matriculationId", "Matriculation ID must be an integer between 0000001 and 9999999.")
      }else {
        if(matriculationId.length != 7){
          errors :+= SimpleError("matriculationId", "Matriculation ID must be a string of length 7.")
        }
      }
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
  override def checkProtectedFields(user: User): Seq[SimpleError] = {
    if(!user.isInstanceOf[Student]){
      throw new Exception("Tried to parse a non-Student as Student.")
    }
    val student = user.asInstanceOf[Student]
    var errors = super.checkProtectedFields(user).asInstanceOf[List[SimpleError]]

    if (matriculationId != student.matriculationId){
      errors :+= SimpleError("matriculationId", "Matriculation ID may not be manually changed.")
    }
    errors
  }

  /**
   * Compares the object against the user parameter to find out if fields, which cannot be changed, are different.
   * Returns a list of SimpleErrors[[SimpleError]]
   *
   * @param user to be checked
   * @return Filled Sequence of [[SimpleError]]
   * */
  override def checkUneditableFields(user: User): Seq[SimpleError] = {
    if(!user.isInstanceOf[Student]){
      throw new Exception("Tried to parse a non-Student as Student.")
    }
    val student = user.asInstanceOf[Student]
    var errors = super.checkUneditableFields(student).asInstanceOf[List[SimpleError]]

    if (latestImmatriculation != student.latestImmatriculation) {
      errors :+= SimpleError("latestImmatriculation", "Latest Immatriculation must not be changed.")
    }

    errors
  }
}

object Student {
  implicit val format: Format[Student] = Json.format
}
