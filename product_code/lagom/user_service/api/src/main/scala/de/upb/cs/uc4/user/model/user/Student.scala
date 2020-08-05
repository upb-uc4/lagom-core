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
                   birthDate: String,
                   latestImmatriculation: String,
                   matriculationId: String) extends User {

  def trim: Student = {
    copy(username.trim, role, address.trim, firstName.trim, lastName.trim,
      picture.trim, email.trim, birthDate.trim, latestImmatriculation.trim, matriculationId.trim)
  }

  def clean: Student = {
    trim.copy(email = email.toLowerCase)
  }

  def toPublic: Student = {
    copy(address = Address.empty, birthDate = "", latestImmatriculation = "", matriculationId = "")
  }

  /** @inheritdoc */
  override def validate: Seq[SimpleError] = {
    val semsterRegex = """(WS[1-9][0-9]{3}\/[0-9]{2})|(SS[1-9][0-9]{3})""".r

    var errors = super.validate.asInstanceOf[List[SimpleError]]

    if(latestImmatriculation != ""){
      if(!semsterRegex.matches(latestImmatriculation)){
        errors :+= SimpleError("latestImmatriculation", "Latest Immatriculation must be a semester of the format \"SSyyyy\" for summer, \"WSyyyy/yy\" for winter.")
      }else{
        if (latestImmatriculation.substring(0,2) == "WS" && (latestImmatriculation.substring(4, 6).toInt+1 == latestImmatriculation.substring(7,9).toInt)){
          errors :+= SimpleError("latestImmatriculation", "Winter semester must consist of two consecutive years.")
        }
      }
    }

    if(matriculationId.isEmpty) {
      errors :+= SimpleError("matriculationId", "Matriculation ID must not be empty.")
    }else{
      if(!(matriculationId forall Character.isDigit) || !(matriculationId.toInt > 0) || !(matriculationId.toInt < 10000000)) {
        errors :+= SimpleError("matriculationId", "Matriculation ID must be an integer between 1 and 9999999.")
      }
    }
    errors
  }


  /** 
    * Compares the object against the user parameter to find out if fields, which should only be changed by users with elevated privileges, are different.
    * Returns a list of [[SimpleError]]
    * 
    * @param user 
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
   * @param user
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
