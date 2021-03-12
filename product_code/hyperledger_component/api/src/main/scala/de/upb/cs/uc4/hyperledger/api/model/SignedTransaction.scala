package de.upb.cs.uc4.hyperledger.api.model

import de.upb.cs.uc4.hyperledger.api.JWTClaimParser
import play.api.libs.json.{ Format, Json }

import java.util.Base64

case class SignedTransaction(unsignedTransactionJwt: String, signature: String) {

  lazy val unsignedTransactionAsByteArray: Array[Byte] =
    Base64.getDecoder.decode(JWTClaimParser.readClaim("unsignedBytes", unsignedTransactionJwt))

  def signatureAsByteArray: Array[Byte] = Base64.getDecoder.decode(signature)
}

object SignedTransaction {
  implicit val format: Format[SignedTransaction] = Json.format
}
