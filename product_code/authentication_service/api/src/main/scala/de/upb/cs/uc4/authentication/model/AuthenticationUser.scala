package de.upb.cs.uc4.authentication.model

import de.upb.cs.uc4.authentication.model.AuthenticationRole.AuthenticationRole
import de.upb.cs.uc4.shared.client.exceptions.SimpleError
import play.api.libs.json.{ Format, Json }

import scala.concurrent.{ ExecutionContext, Future }

/** Used to be sent to the AuthenticationService. Includes only relevant data for that service. */
case class AuthenticationUser(username: String, password: String, role: AuthenticationRole) {

  def trim: AuthenticationUser = {
    copy(username.trim)
  }

  def clean: AuthenticationUser = {
    trim
  }

  def validate(implicit ec: ExecutionContext): Future[Seq[SimpleError]] = Future {
    val usernameRegex = """[a-zA-Z0-9-.]{4,16}""".r

    var errors = List[SimpleError]()
    if (!usernameRegex.matches(username)) {
      errors :+= SimpleError(
        "username",
        "Username must consist of 4 to 16 characters, and must only contain letters, numbers, '-', and '.'."
      )
    }
    if (password == "") {
      errors :+= SimpleError("password", "Password must not be empty.")
    }
    errors
  }
}

object AuthenticationUser {
  implicit val format: Format[AuthenticationUser] = Json.format
}
