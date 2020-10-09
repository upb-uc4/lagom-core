package de.upb.cs.uc4.certificate.model

import play.api.libs.json.{ Format, Json }

case class EncryptedPrivateKey(key: String, iv: String, salt: String)

object EncryptedPrivateKey {
  implicit val format: Format[EncryptedPrivateKey] = Json.format
}
