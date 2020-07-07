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
                 birthDate: String) extends User {

  def trim: Admin = {
    copy(username.trim, role, address.trim, firstName.trim, lastName.trim,
      picture.trim, email.trim, birthDate.trim)
  }
}

object Admin {
  implicit val format: Format[Admin] = Json.format
}
