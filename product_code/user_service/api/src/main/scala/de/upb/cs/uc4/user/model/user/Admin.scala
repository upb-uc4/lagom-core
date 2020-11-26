package de.upb.cs.uc4.user.model.user

import de.upb.cs.uc4.user.model.Address
import de.upb.cs.uc4.user.model.Role.Role
import play.api.libs.json.{ Format, Json }

case class Admin(
    username: String,
    enrollmentIdSecret: String,
    isActive: Boolean,
    role: Role,
    address: Address,
    firstName: String,
    lastName: String,
    email: String,
    phoneNumber: String,
    birthDate: String
) extends User {

  def copyUser(
      username: String = this.username,
      enrollmentIdSecret: String = this.enrollmentIdSecret,
      isActive: Boolean,
      role: Role = this.role,
      address: Address = this.address,
      firstName: String = this.firstName,
      lastName: String = this.lastName,
      email: String = this.email,
      phoneNumber: String = this.phoneNumber,
      birthDate: String = this.birthDate
  ): Admin =
    copy(username, enrollmentIdSecret, isActive, role, address, firstName, lastName, email, phoneNumber, birthDate)

  override def trim: Admin = super.trim.asInstanceOf[Admin]

  override def toPublic: Admin =
    Admin(this.username, "", this.isActive, this.role, Address.empty, this.firstName, this.lastName, this.email, this.phoneNumber, "")

  override def clean: Admin = super.clean.asInstanceOf[Admin]

  /** @inheritdoc */
  override def softDelete: Admin = {
    Admin(this.username, "", isActive = false, this.role, Address.empty, "", "", "", "", "")
  }
}

object Admin {
  implicit val format: Format[Admin] = Json.format
}
