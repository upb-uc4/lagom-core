package de.upb.cs.uc4.certificate.model

import play.api.libs.json.{ Format, Json }

case class UsernameEnrollmentIdPair(username: String, enrollmentId: String)

object UsernameEnrollmentIdPair {
  implicit val format: Format[UsernameEnrollmentIdPair] = Json.format
}