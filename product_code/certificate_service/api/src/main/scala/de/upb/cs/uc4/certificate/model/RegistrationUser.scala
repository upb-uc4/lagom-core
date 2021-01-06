package de.upb.cs.uc4.certificate.model

import play.api.libs.json.{ Format, Json }

case class RegistrationUser(enrollmentId: String, role: String)

object RegistrationUser {
  implicit val format: Format[RegistrationUser] = Json.format
}