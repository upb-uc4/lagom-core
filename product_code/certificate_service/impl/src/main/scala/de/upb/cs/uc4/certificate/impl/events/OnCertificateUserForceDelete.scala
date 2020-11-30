package de.upb.cs.uc4.certificate.impl.events

import de.upb.cs.uc4.user.model.Role.Role
import play.api.libs.json.{ Format, Json }

case class OnCertificateUserForceDelete(username: String, role: Role) extends CertificateEvent

object OnCertificateUserForceDelete {
  implicit val format: Format[OnCertificateUserForceDelete] = Json.format
}