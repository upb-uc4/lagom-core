package de.upb.cs.uc4.user.model.user

import de.upb.cs.uc4.shared.client.SimpleError
import de.upb.cs.uc4.user.model.Role.Role
import de.upb.cs.uc4.user.model.{Address, Role}
import play.api.libs.json.{Format, JsResult, JsValue, Json}

trait User {
  val username: String
  val role: Role
  val address: Address
  val firstName: String
  val lastName: String
  val picture: String
  val email: String
  val birthDate: String


  def trim: User

  def validate: Seq[SimpleError] = {
    
    val generalRegex = """[\s\S]+""".r // Allowed characters for general strings TBD
    val usernameRegex = """[a-zA-Z0-9-]+""".r
    val nameRegex = """[a-zA-Z-]+""".r
    val mailRegex = """[a-zA-Z0-9\Q.-_,\E]+@[a-zA-Z0-9\Q.-_,\E]+\.[a-zA-Z]+""".r
    val dateRegex = """(\d\d\d\d)-(\d\d)-(\d\d)""".r
    

    var errors = List[SimpleError]()

    if (!usernameRegex.matches(username)) {
      errors :+= SimpleError("username","Username may only contain [..].")
    }

    if (!Role.All.contains(role)) { //optUser check to ensure this is during creation
      errors :+= SimpleError("role", "Role must be one of [..]" + Role.All + ".")
    }

    errors ++= address.validate.map(error => SimpleError("address." + error.name, error.reason))

    if (!mailRegex.matches(email)) {
      errors :+= SimpleError("email", "Email must be in email format example@xyz.com.")
    }
    if (!dateRegex.matches(birthDate)) {
      errors :+= SimpleError("birthDate", "Birthdate must be of the following format \"yyyy-mm-dd\".")
    }
    if (!nameRegex.matches(firstName)) {
      errors :+= SimpleError("firstName", "First name must only contain letters or '-'.")
    }
    if (!nameRegex.matches(lastName)) {
      errors :+= SimpleError("lastName", "Last name must only contain letters or '-'.")
    }
    if (!generalRegex.matches(picture)) { //TODO, this does not make any sense, but pictures are not defined yet
      errors :+= SimpleError("picture", "Picture is invalid.")
    }
    errors
  }
}

object User {
  implicit val format: Format[User] = new Format[User] {
    override def reads(json: JsValue): JsResult[User] = {
      json("role").as[Role] match {
        case Role.Admin => Json.fromJson[Admin](json)
        case Role.Student => Json.fromJson[Student](json)
        case Role.Lecturer => Json.fromJson[Lecturer](json)
      }
    }

    override def writes(o: User): JsValue = {
      o match {
        case admin: Admin => Json.toJson(admin)
        case student: Student => Json.toJson(student)
        case lecturer: Lecturer => Json.toJson(lecturer)
      }
    }
  }
}