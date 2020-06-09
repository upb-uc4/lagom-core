package de.upb.cs.uc4.authentication.model

import play.api.libs.json.{Format, Json}

/** The different AuthenticationResponses of the AuthenticationService  */
object AuthenticationResponse extends Enumeration {
  type AuthenticationResponse = Value
  val Correct, WrongUsername, WrongPassword, NotAuthorized = Value

  implicit val format: Format[AuthenticationResponse] = Json.formatEnum(this)
}
