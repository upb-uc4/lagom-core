package de.upb.cs.uc4.user.model.user

import de.upb.cs.uc4.shared.client.configuration.{ ErrorMessageCollection, RegexCollection }
import de.upb.cs.uc4.shared.client.exceptions.SimpleError
import de.upb.cs.uc4.user.model.Address
import de.upb.cs.uc4.user.model.Role.Role
import play.api.libs.json.{ Format, Json }

import scala.concurrent.{ ExecutionContext, Future }

case class Lecturer(
    username: String,
    enrollmentIdSecret: String,
    isActive: Boolean,
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
      enrollmentIdSecret: String = this.enrollmentIdSecret,
      isActive: Boolean = this.isActive,
      role: Role = this.role,
      address: Address = this.address,
      firstName: String = this.firstName,
      lastName: String = this.lastName,
      email: String = this.email,
      phoneNumber: String = this.phoneNumber,
      birthDate: String = this.birthDate
  ): Lecturer =
    copy(username, enrollmentIdSecret, isActive, role, address, firstName, lastName, email, phoneNumber, birthDate)

  override def trim: Lecturer =
    super.trim.asInstanceOf[Lecturer].copy(freeText = freeText.trim, researchArea = researchArea.trim)

  override def toPublic: Lecturer =
    Lecturer(this.username, "", this.isActive, this.role, Address.empty, this.firstName, this.lastName, this.email, this.phoneNumber, "", this.freeText, this.researchArea)

  override def clean: Lecturer = super.clean.asInstanceOf[Lecturer]

  /** @inheritdoc */
  override def validate(implicit ec: ExecutionContext): Future[Seq[SimpleError]] = {
    val freeTextRegex = RegexCollection.Commons.longTextRegex
    val researchAreaRegex = RegexCollection.Lecturer.researchAreaRegex

    val freeTextMessage = ErrorMessageCollection.Commons.longTextMessage
    val researchAreaMessage = ErrorMessageCollection.Lecturer.researchAreaMessage

    super.validate.map { superErrors =>
      var errors = superErrors.toList
      if (!freeTextRegex.matches(freeText)) {
        errors :+= SimpleError("freeText", freeTextMessage)
      }
      if (!researchAreaRegex.matches(researchArea)) {
        errors :+= SimpleError("researchArea", researchAreaMessage)
      }
      errors
    }
  }

  /** @inheritdoc */
  override def softDelete: Lecturer = {
    Lecturer(this.username, "", isActive = false, this.role, Address.empty, this.firstName, this.lastName, "", "", "", "", "")
  }
}

object Lecturer {
  implicit val format: Format[Lecturer] = Json.format
}
