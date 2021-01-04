package de.upb.cs.uc4.shared.client

import java.util.Base64

import play.api.libs.json.{ Format, Json }

case class SignedTransaction(unsignedTransaction: String, signature: String) {

  def unsignedTransactionAsByteArray: Array[Byte] = Base64.getDecoder.decode(unsignedTransaction)

  def signatureAsByteArray: Array[Byte] = Base64.getDecoder.decode(signature)
}

object SignedTransaction {
  implicit val format: Format[SignedTransaction] = Json.format
}
