package de.upb.cs.uc4.certificate.model

import de.upb.cs.uc4.shared.client.configuration.{ ErrorMessageCollection, RegexCollection }
import de.upb.cs.uc4.shared.client.exceptions.SimpleError
import play.api.libs.json.{ Format, Json }

import scala.concurrent.{ ExecutionContext, Future }

case class PostMessageCSR(certificateSigningRequest: String, encryptedPrivateKey: EncryptedPrivateKey) {

  def validate(implicit ec: ExecutionContext): Future[Seq[SimpleError]] = encryptedPrivateKey.validate.map { result =>
    var errors = List[SimpleError]()

    val csrRegex = RegexCollection.PostMessageCSR.csrRegex
    val csrMessage = ErrorMessageCollection.PostMessageCSR.csrMessage

    if (!csrRegex.matches(certificateSigningRequest)) {
      errors :+= SimpleError("certificateSigningRequest", csrMessage)
    }

    errors ++ result
  }
}

object PostMessageCSR {
  implicit val format: Format[PostMessageCSR] = Json.format
}
