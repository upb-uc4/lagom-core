package de.upb.cs.uc4.authentication.model

import de.upb.cs.uc4.authentication.model.AuthenticationRole.AuthenticationRole
import de.upb.cs.uc4.shared.client.exceptions.SimpleError
import play.api.libs.json.{Format, Json}

/** Used to be sent to the AuthenticationService. Includes only relevant data for that service. */
case class AuthenticationUser(username: String, password: String, role: AuthenticationRole) {

  def validate : Seq[SimpleError] = {
    val usernameRegex = """[a-zA-Z0-9-]+""".r

    var errors = List[SimpleError]()
    if (!usernameRegex.matches(username)) {
      errors :+= SimpleError("username","Username may only contain [..].")
    }
    if (password.trim == ""){
      errors :+= SimpleError("password","Password must not be empty.")
    }
    errors
  }
}

object AuthenticationUser {
  implicit val format: Format[AuthenticationUser] = Json.format
}
