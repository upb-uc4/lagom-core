package de.upb.cs.uc4.hyperledger.api.model

import play.api.libs.json.{ Format, Json }

import java.util.Base64

case class SignedTransaction(unsignedTransaction: String, signature: String) {

  def unsignedTransactionAsByteArray: Array[Byte] = Base64.getDecoder.decode(unsignedTransaction)

  def signatureAsByteArray: Array[Byte] = Base64.getDecoder.decode(signature)
}

object SignedTransaction {
  implicit val format: Format[SignedTransaction] = Json.format
}
