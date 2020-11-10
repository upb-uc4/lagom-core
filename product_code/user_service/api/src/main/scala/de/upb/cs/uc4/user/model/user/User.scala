package de.upb.cs.uc4.user.model.user

import com.fasterxml.jackson.annotation.JsonTypeInfo
import de.upb.cs.uc4.shared.client.configuration.RegexCollection
import de.upb.cs.uc4.shared.client.exceptions.SimpleError
import de.upb.cs.uc4.user.model.Role.Role
import de.upb.cs.uc4.user.model.{ Address, Role }
import play.api.libs.json.{ Format, JsResult, JsValue, Json }

import scala.concurrent.{ ExecutionContext, Future }

@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
trait User {
  val username: String
  val isActive: Boolean
  val role: Role
  val address: Address
  val firstName: String
  val lastName: String
  val email: String
  val phoneNumber: String
  val birthDate: String

  def copyUser(
      username: String = this.username,
      isActive: Boolean = this.isActive,
      role: Role = this.role,
      address: Address = this.address,
      firstName: String = this.firstName,
      lastName: String = this.lastName,
      email: String = this.email,
      phoneNumber: String = this.phoneNumber,
      birthDate: String = this.birthDate
  ): User

  def toPublic: User

  def trim: User = copyUser(
    username.trim, isActive, role, address.trim, firstName.trim, lastName.trim,
    email.trim, phoneNumber.trim, birthDate.trim
  )

  def clean: User = trim.copyUser(email = email.toLowerCase, phoneNumber = phoneNumber.replaceAll("\\s+", ""))

  /** Validates the object by checking predefined conditions like correct charsets, syntax, etc.
    * Returns a list of SimpleErrors[[SimpleError]]
    *
    * @return Filled Sequence of [[SimpleError]]
    */
  def validate(implicit ec: ExecutionContext): Future[Seq[SimpleError]] = {

    val usernameRegex = RegexCollection.User.usernameRegex
    val nameRegex = RegexCollection.Commons.nameRegex
    val mailRegex = RegexCollection.User.mailRegex
    val phoneNumberRegex = RegexCollection.User.phoneNumberRegex
    val dateRegex = RegexCollection.Commons.dateRegex

    address.validate.map { addressErrors =>
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

      errors ++= addressErrors.map(error => SimpleError("address." + error.name, error.reason))

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
      errors
    }
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
    if (isActive != user.isActive) {
      errors :+= SimpleError("isActive", "IsActive must not be changed.")
    }

    errors
  }

  /** Creates a copy of this user, with most personal info deleted
    *
    * @return A new user, with (most) personal info deleted
    */
  def softDelete: User = {
    copyUser(isActive = false, address = Address.empty, email = "", phoneNumber = "", birthDate = "")
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