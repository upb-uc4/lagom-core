package de.upb.cs.uc4.user.model.user

import de.upb.cs.uc4.shared.client.SimpleError
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
      picture.trim, email.trim, birthDate.trim, freeText.trim, researchArea.trim)
  }

  override def validate: Seq[SimpleError] = {
    val generalRegex = """[\s\S]+""".r // Allowed characters for general strings TBD

    var errors = List[SimpleError]()
    errors ++= super.validate
    if (!generalRegex.matches(freeText)) {
      errors :+= SimpleError("freeText", "Free text must only contain the following characters: [..].")
    }
    if (!generalRegex.matches(researchArea)) {
      errors :+= SimpleError("researchArea", "Research area must only contain the following characters [..].")
    }
    errors
  }
  def checkEditableFields (user: Lecturer): Seq[SimpleError] = {

    var errors = List[SimpleError]()
    errors ++= super.checkEditableFields(user)
    errors
  }
}



object Lecturer {
  implicit val format: Format[Lecturer] = Json.format
}
