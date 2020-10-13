package de.upb.cs.uc4.certificate.model

import de.upb.cs.uc4.shared.client.exceptions.SimpleError
import play.api.libs.json.{ Format, Json }

import scala.concurrent.{ ExecutionContext, Future }

case class EncryptedPrivateKey(key: String, iv: String, salt: String) {

  def validate(implicit ec: ExecutionContext): Future[Seq[SimpleError]] = Future {
    val keyRegex = """[\s\S]{1,8192}""".r
    val ivRegex = """[\s\S]{1,64}""".r
    val saltRegex = """[\s\S]{1,256}""".r

    if (key.isEmpty && iv.isEmpty && salt.isEmpty) {
      Seq()
    }
    else if (Seq(key, iv, salt).contains("")) {
      Seq(SimpleError("encryptedPrivateKey", "Either all fields must be empty or no fields must be empty."))
    }
    else {
      var errors = List[SimpleError]()

      if (!keyRegex.matches(key)) {
        errors :+= SimpleError("key", "The key must not be empty or longer than 8192 characters.")
      }
      if (!ivRegex.matches(iv)) {
        errors :+= SimpleError("iv", "The iv must not be empty or longer than 64 characters.")
      }
      if (!saltRegex.matches(salt)) {
        errors :+= SimpleError("salt", "The salt must not be empty or longer than 256 characters.")
      }

      errors
    }
  }
}

object EncryptedPrivateKey {
  implicit val format: Format[EncryptedPrivateKey] = Json.format
}
