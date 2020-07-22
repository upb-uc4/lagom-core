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

  /** @inheritdoc */
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


  /** 
    * Compares the object against the user parameter to find out if fields, which should only be changed by users with elevated privileges, are different.
    * Returns a list of SimpleErrors[[de.upb.cs.uc4.shared.client.SimpleError]]
    * 
    * @param user 
    * @return Filled Sequence of [[de.upb.cs.uc4.shared.client.SimpleError]]
    */
  def checkEditableFields (user: Student): Seq[SimpleError] = {

    var errors = List[SimpleError]()
   
    errors ++= super.checkEditableFields(user)
    
    if (immatriculationStatus != user.immatriculationStatus){
      errors :+= SimpleError("immatriculationStatus", "Immatriculation status may not be manually changed.")
    }
    if (matriculationId != user.matriculationId){
      errors :+= SimpleError("matriculationId", "Matriculation ID may not be manually changed.")
    }
    if (semesterCount != user.semesterCount){
      errors :+= SimpleError("semesterCount", "Number of semesters may not be manually changed.")
    }
    if (fieldsOfStudy != user.fieldsOfStudy){
      errors :+= SimpleError("fieldsOfStudy", "Fields of study may not be manually changed.")
    }
    errors
  }
}

object Student {
  implicit val format: Format[Student] = Json.format
}
