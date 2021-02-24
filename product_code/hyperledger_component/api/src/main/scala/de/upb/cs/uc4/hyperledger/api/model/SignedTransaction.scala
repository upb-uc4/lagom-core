package de.upb.cs.uc4.hyperledger.api.model

import io.jsonwebtoken.Jwts
import play.api.libs.json.{ Format, Json }

import java.util.Base64

case class SignedTransaction(unsignedTransactionJwt: String, signature: String) {

  def unsignedTransactionAsByteArray: Array[Byte] = {
    val claims = Jwts.parser().parseClaimsJws(unsignedTransactionJwt).getBody
    Base64.getDecoder.decode(claims.get("unsignedBytes", classOf[String]))
  }

  def signatureAsByteArray: Array[Byte] = Base64.getDecoder.decode(signature)
}

object SignedTransaction {
  implicit val format: Format[SignedTransaction] = Json.format
}
