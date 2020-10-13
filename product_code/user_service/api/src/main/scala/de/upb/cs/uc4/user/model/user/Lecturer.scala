package de.upb.cs.uc4.user.model.user

import de.upb.cs.uc4.shared.client.exceptions.SimpleError
import de.upb.cs.uc4.user.model.Address
import de.upb.cs.uc4.user.model.Role.Role
import play.api.libs.json.{ Format, Json }

import scala.concurrent.{ ExecutionContext, Future }

case class Lecturer(
    username: String,
    role: Role,
    address: Address,
    firstName: String,
    lastName: String,
    email: String,
    phoneNumber: String,
    birthDate: String,
    freeText: String,
    researchArea: String
) extends User {

  def copyUser(
      username: String = this.username,
      role: Role = this.role,
      address: Address = this.address,
      firstName: String = this.firstName,
      lastName: String = this.lastName,
      email: String = this.email,
      phoneNumber: String = this.phoneNumber,
      birthDate: String = this.birthDate
  ): Lecturer =
    copy(username, role, address, firstName, lastName, email, phoneNumber, birthDate)

  override def trim: Lecturer =
    super.trim.asInstanceOf[Lecturer].copy(freeText = freeText.trim, researchArea = researchArea.trim)

  override def toPublic: Lecturer = super.toPublic.asInstanceOf[Lecturer]

  override def clean: Lecturer = super.clean.asInstanceOf[Lecturer]

  /** @inheritdoc */
  override def validate(implicit ec: ExecutionContext): Future[Seq[SimpleError]] = {
    val freeTextRegex = """[\s\S]{0,10000}""".r
    val researchAreaRegex = """[\s\S]{0,200}""".r

    super.validate.map { superErrors =>
      var errors = superErrors.toList
      if (!freeTextRegex.matches(freeText)) {
        errors :+= SimpleError("freeText", "Free text must contain 0 to 10000 characters.")
      }
      if (!researchAreaRegex.matches(researchArea)) {
        errors :+= SimpleError("researchArea", "Research area must contain 0 to 200 characters.")
      }
      errors
    }
  }
}

object Lecturer {
  implicit val format: Format[Lecturer] = Json.format
}
