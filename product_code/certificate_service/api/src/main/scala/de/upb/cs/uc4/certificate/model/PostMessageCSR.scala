package de.upb.cs.uc4.certificate.model

import de.upb.cs.uc4.shared.client.exceptions.SimpleError
import play.api.libs.json.{ Format, Json }

case class PostMessageCSR(certificateSigningRequest: String, encryptedPrivateKey: EncryptedPrivateKey) {

  def validate: Seq[SimpleError] = {
    val allRegex = """[\s\S]{1,2000}""".r

    var errors = List[SimpleError]()

    encryptedPrivateKey match {
      case EncryptedPrivateKey("", "", "") =>
      case EncryptedPrivateKey(allRegex(_), allRegex(_), allRegex(_)) =>
      case _ =>
        errors :+= SimpleError("encryptedPrivateKey", "Either all fields must be empty or no fields must be empty.")
    }
    errors
  }
}

object PostMessageCSR {
  implicit val format: Format[PostMessageCSR] = Json.format
}
