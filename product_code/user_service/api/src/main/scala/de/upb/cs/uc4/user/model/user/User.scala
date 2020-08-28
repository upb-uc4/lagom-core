package de.upb.cs.uc4.user.model.user

import de.upb.cs.uc4.shared.client.exceptions.SimpleError
import de.upb.cs.uc4.user.model.Role.Role
import de.upb.cs.uc4.user.model.{ Address, Role }
import play.api.libs.json.{ Format, JsResult, JsValue, Json }

trait User {
  val username: String
  val role: Role
  val address: Address
  val firstName: String
  val lastName: String
  val picture: String
  val email: String
  val phoneNumber: String
  val birthDate: String

  def copyUser(
      username: String = this.username,
      role: Role = this.role,
      address: Address = this.address,
      firstName: String = this.firstName,
      lastName: String = this.lastName,
      picture: String = this.picture,
      email: String = this.email,
      phoneNumber: String = this.phoneNumber,
      birthDate: String = this.birthDate
  ): User

  def trim: User = copyUser(
    username.trim, role, address.trim, firstName.trim, lastName.trim,
    picture.trim, email.trim, phoneNumber.trim, birthDate.trim
  )

  def clean: User = trim.copyUser(email = email.toLowerCase, phoneNumber = phoneNumber.replaceAll("\\s+", ""))

  def toPublic: User = copyUser(address = Address.empty, birthDate = "")

  /** Validates the object by checking predefined conditions like correct charsets, syntax, etc.
    * Returns a list of SimpleErrors[[SimpleError]]
    *
    * @return Filled Sequence of [[SimpleError]]
    */
  def validate: Seq[SimpleError] = {

    val generalRegex = """[\s\S]{0,200}""".r // Allowed characters for general strings TBD
    val usernameRegex = """[a-zA-Z0-9-.]{4,16}""".r
    val nameRegex = """[\s\S]{1,100}""".r
    val mailRegex = """(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|"(?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21\x23-\x5b\x5d-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])*")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\[(?:(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9]))\.){3}(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9])|[a-z0-9-]*[a-z0-9]:(?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21-\x5a\x53-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])+)])""".r
    val phoneNumberRegex = """\+[0-9]{1,30}""".r

    val dateRegex = """^(?:(?:(?:(?:(?:[1-9]\d)(?:0[48]|[2468][048]|[13579][26])|(?:(?:[2468][048]|[13579][26])00))(-)(?:0?2\1(?:29)))|(?:(?:[1-9]\d{3})(-)(?:(?:(?:0?[13578]|1[02])\2(?:31))|(?:(?:0?[13-9]|1[0-2])\2(?:29|30))|(?:(?:0?[1-9])|(?:1[0-2]))\2(?:0?[1-9]|1\d|2[0-8])))))$""".r

    var errors = List[SimpleError]()

    if (!usernameRegex.matches(username)) {
      errors :+= SimpleError(
        "username",
        "Username must consist of 4 to 16 characters, and must only contain letters, numbers, '-', and '.'."
      )
    }

    if (!Role.All.contains(role)) {
      errors :+= SimpleError("role", "Role must be one of " + Role.All + ".")
    }
    else {
      this match {
        case _: Student => if (role != Role.Student) {
          errors :+= SimpleError("role", "Role must be one of " + Role.All + ", and conform to the type of object.")
        }
        case _: Lecturer => if (role != Role.Lecturer) {
          errors :+= SimpleError("role", "Role must be one of " + Role.All + ", and conform to the type of object.")
        }
        case _: Admin => if (role != Role.Admin) {
          errors :+= SimpleError("role", "Role must be one of " + Role.All + ", and conform to the type of object.")
        }
      }
    }

    errors ++= address.validate.map(error => SimpleError("address." + error.name, error.reason))

    if (!mailRegex.matches(email)) {
      errors :+= SimpleError("email", "Email must be in email format example@xyz.com.")
    }
    if (!dateRegex.matches(birthDate)) {
      errors :+= SimpleError("birthDate", "Birthdate must be of the following format \"yyyy-mm-dd\".")
    }
    if (!phoneNumberRegex.matches(phoneNumber)) {
      errors :+= SimpleError("phoneNumber", "Phone number must be of the following format \"+xxxxxxxxxxxx\".")
    }
    if (!nameRegex.matches(firstName)) {
      errors :+= SimpleError("firstName", "First name must contain between 1 and 100 characters.")
    }
    if (!nameRegex.matches(lastName)) {
      errors :+= SimpleError("lastName", "Last name must contain between 1 and 100 characters.")
    }
    if (!generalRegex.matches(picture)) { //TODO, this does not make any sense, but pictures are not defined yet
      errors :+= SimpleError("picture", "Picture is invalid.")
    }
    errors
  }

  /** Compares the object against the user parameter to find out if fields, which should only be changed by users with elevated privileges, are different.
    * Returns a list of SimpleErrors[[SimpleError]]
    *
    * @param user to be checked
    * @return Filled Sequence of [[SimpleError]]
    */
  def checkProtectedFields(user: User): Seq[SimpleError] = {
    var errors = List[SimpleError]()

    if (firstName != user.firstName) {
      errors :+= SimpleError("firstName", "First name may not be manually changed.")
    }
    if (lastName != user.lastName) {
      errors :+= SimpleError("lastName", "Last name may not be manually changed.")
    }
    if (birthDate != user.birthDate) {
      errors :+= SimpleError("birthDate", "Birthdate may not be manually changed.")
    }
    errors
  }

  /** Compares the object against the user parameter to find out if fields, which cannot be changed, are different.
    * Returns a list of SimpleErrors[[SimpleError]]
    *
    * @param user to be checked
    * @return Filled Sequence of [[SimpleError]]
    */
  def checkUneditableFields(user: User): Seq[SimpleError] = {
    var errors = List[SimpleError]()

    if (role != user.role) {
      errors :+= SimpleError("role", "Role must not be changed.")
    }

    errors
  }
}

object User {
  implicit val format: Format[User] = new Format[User] {
    override def reads(json: JsValue): JsResult[User] = {
      json("role").as[Role] match {
        case Role.Admin    => Json.fromJson[Admin](json)
        case Role.Student  => Json.fromJson[Student](json)
        case Role.Lecturer => Json.fromJson[Lecturer](json)
      }
    }

    override def writes(o: User): JsValue = {
      o match {
        case admin: Admin       => Json.toJson(admin)
        case student: Student   => Json.toJson(student)
        case lecturer: Lecturer => Json.toJson(lecturer)
      }
    }
  }
}