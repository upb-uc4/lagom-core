package de.upb.cs.uc4.certificate.impl.events

import de.upb.cs.uc4.user.model.Role.Role
import play.api.libs.json.{ Format, Json }

case class OnCertificateUserDelete(username: String, role: Role) extends CertificateEvent

object OnCertificateUserDelete {
  implicit val format: Format[OnCertificateUserDelete] = Json.format
}
