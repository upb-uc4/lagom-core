package de.upb.cs.uc4.certificate.impl.events

import play.api.libs.json.{ Format, Json }

case class OnCertificateUserDelete(username: String) extends CertificateEvent

object OnCertificateUserDelete {
  implicit val format: Format[OnCertificateUserDelete] = Json.format
}
