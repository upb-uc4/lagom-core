package de.upb.cs.uc4.authentication.model

import de.upb.cs.uc4.authentication.model.AuthenticationRole.AuthenticationRole
import de.upb.cs.uc4.shared.client.configuration.{ErrorMessageCollection, RegexCollection}
import de.upb.cs.uc4.shared.client.exceptions.SimpleError
import play.api.libs.json.{Format, Json}

import scala.concurrent.{ExecutionContext, Future}

/** Used to be sent to the AuthenticationService. Includes only relevant data for that service. */
case class AuthenticationUser(username: String, password: String, role: AuthenticationRole) {

  def trim: AuthenticationUser = {
    copy(username.trim)
  }

  def clean: AuthenticationUser = {
    trim
  }

  def validate(implicit ec: ExecutionContext): Future[Seq[SimpleError]] = Future {
    val usernameRegex = RegexCollection.AuthenticationUser.usernameRegex
    val passwordRegex = RegexCollection.AuthenticationUser.passwordRegex
    val usernameError = ErrorMessageCollection.AuthenticationUser.usernameMessage
    val passwordError = ErrorMessageCollection.AuthenticationUser.passwordMessage

    var errors = List[SimpleError]()
    if (!usernameRegex.matches(username)) {
      errors :+= SimpleError(
        "username",
        usernameError
      )
    }
    if (!passwordRegex.matches(password)) {
      errors :+= SimpleError("password", passwordError)
    }
    errors
  }
}

object AuthenticationUser {
  implicit val format: Format[AuthenticationUser] = Json.format
}
