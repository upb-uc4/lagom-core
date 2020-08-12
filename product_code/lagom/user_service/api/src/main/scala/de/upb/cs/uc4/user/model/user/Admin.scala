package de.upb.cs.uc4.user.model.user

import de.upb.cs.uc4.user.model.Address
import de.upb.cs.uc4.user.model.Role.Role
import play.api.libs.json.{Format, Json}

case class Admin(username: String,
                 role: Role,
                 address: Address,
                 firstName: String,
                 lastName: String,
                 picture: String,
                 email: String,
                 phoneNumber: String,
                 birthDate: String) extends User {

  def copyUser(username: String = this.username,
               role: Role = this.role,
               address: Address = this.address,
               firstName: String = this.firstName,
               lastName: String = this.lastName,
               picture: String = this.picture,
               email: String = this.email,
               phoneNumber: String = this.phoneNumber,
               birthDate: String = this.birthDate): Admin =
    copy(username, role, address, firstName, lastName, picture, email, phoneNumber, birthDate)

  override def trim: Admin = super.clean.asInstanceOf[Admin]

  override def toPublic: Admin = super.clean.asInstanceOf[Admin]

  override def clean: Admin = super.clean.asInstanceOf[Admin]
}

object Admin {
  implicit val format: Format[Admin] = Json.format
}
