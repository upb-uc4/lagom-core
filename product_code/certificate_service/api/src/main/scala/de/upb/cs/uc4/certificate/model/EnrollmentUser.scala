package de.upb.cs.uc4.certificate.model

import play.api.libs.json.{ Format, Json }

case class EnrollmentUser(enrollmentId: String, role: String)

object EnrollmentUser {
  implicit val format: Format[EnrollmentUser] = Json.format
}