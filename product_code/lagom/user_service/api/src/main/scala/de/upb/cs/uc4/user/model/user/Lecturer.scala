package de.upb.cs.uc4.user.model.user

import de.upb.cs.uc4.user.model.Address
import de.upb.cs.uc4.user.model.Role.Role
import play.api.libs.json.{Format, Json}

case class Lecturer(username: String,
                    role: Role,
                    address: Address,
                    firstName: String,
                    lastName: String,
                    picture: String,
                    email: String,
                    birthDate: String,
                    freeText: String,
                    researchArea: String) extends User {

  def trim: Lecturer = {
    copy(username.trim, role, address.trim, firstName.trim, lastName.trim,
      picture.trim, email.trim, freeText.trim, researchArea.trim)
  }
}


object Lecturer {
  implicit val format: Format[Lecturer] = Json.format
}
