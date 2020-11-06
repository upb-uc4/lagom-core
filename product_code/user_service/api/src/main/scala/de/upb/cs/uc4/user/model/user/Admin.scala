package de.upb.cs.uc4.user.model.user

import de.upb.cs.uc4.user.model.Address
import de.upb.cs.uc4.user.model.Role.Role
import play.api.libs.json.{ Format, Json }

case class Admin(
    username: String,
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
      role: Role = this.role,
      address: Address = this.address,
      firstName: String = this.firstName,
      lastName: String = this.lastName,
      email: String = this.email,
      phoneNumber: String = this.phoneNumber,
      birthDate: String = this.birthDate
  ): Admin =
    copy(username, role, address, firstName, lastName, email, phoneNumber, birthDate)

  override def trim: Admin = super.trim.asInstanceOf[Admin]

  override def toPublic: Admin =
    Admin(this.username, this.role, Address.empty, this.firstName, this.lastName, this.email, this.phoneNumber, "")

  //override def toPublic: Admin = super.toPublic.asInstanceOf[Admin]

  override def clean: Admin = super.clean.asInstanceOf[Admin]
}

object Admin {
  implicit val format: Format[Admin] = Json.format
}
