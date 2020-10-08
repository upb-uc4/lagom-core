package de.upb.cs.uc4.certificate.impl.events

import de.upb.cs.uc4.certificate.model.EncryptedPrivateKey
import play.api.libs.json.{ Format, Json }

case class OnCertficateAndKeySet(certificate: String, encryptedPrivateKey: EncryptedPrivateKey) extends CertificateEvent

object OnCertficateAndKeySet {
  implicit val format: Format[OnCertficateAndKeySet] = Json.format
}
