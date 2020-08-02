package de.upb.cs.uc4.user.model.user

import de.upb.cs.uc4.shared.client.exceptions.SimpleError
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

  def clean: Lecturer = {
    trim.copy(email = email.toLowerCase)
  }

  /** @inheritdoc */
  override def validate: Seq[SimpleError] = {
    val freeTextRegex = """[\s\S]{0,10000}""".r
    val researchAreaRegex = """[\s\S]{0,200}""".r

    var errors = super.validate.asInstanceOf[List[SimpleError]]
    if (!freeTextRegex.matches(freeText)) {
      errors :+= SimpleError("freeText", "Free text must contain 0 to 10000 characters.")
    }
    if (!researchAreaRegex.matches(researchArea)) {
      errors :+= SimpleError("researchArea", "Research area must contain 0 to 10000 characters.")
    }
    errors
  }
}



object Lecturer {
  implicit val format: Format[Lecturer] = Json.format
}
