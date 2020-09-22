package de.upb.cs.uc4.certificate.model

import play.api.libs.json.{ Format, Json }

case class PostMessageCSR(certificateSigningRequest: String, encryptedPrivateKey: EncryptedPrivateKey)

object PostMessageCSR {
  implicit val format: Format[PostMessageCSR] = Json.format
}
