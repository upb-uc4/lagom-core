package de.upb.cs.uc4.certificate.impl.actor

import de.upb.cs.uc4.certificate.model.EncryptedPrivateKey
import play.api.libs.json.{ Format, Json }

case class CertificateUser(
    enrollmentId: Option[String],
    enrollmentSecret: Option[String],
    certificate: Option[String],
    encryptedPrivateKey: Option[EncryptedPrivateKey]
)

object CertificateUser {
  implicit def format: Format[CertificateUser] = Json.format
}

