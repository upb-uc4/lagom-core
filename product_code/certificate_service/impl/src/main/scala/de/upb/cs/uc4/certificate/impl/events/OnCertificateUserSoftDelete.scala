package de.upb.cs.uc4.certificate.impl.events

import de.upb.cs.uc4.user.model.Role.Role
import play.api.libs.json.{ Format, Json }

case class OnCertificateUserSoftDelete(username: String, role: Role) extends CertificateEvent

object OnCertificateUserSoftDelete {
  implicit val format: Format[OnCertificateUserSoftDelete] = Json.format
}
