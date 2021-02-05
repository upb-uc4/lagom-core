package de.upb.cs.uc4.certificate.impl.events

import play.api.libs.json.{ Format, Json }

case class OnRegisterUser(username: String, enrollmentId: String, enrollmentSecret: String) extends CertificateEvent

object OnRegisterUser {
  implicit val format: Format[OnRegisterUser] = Json.format
}
