package de.upb.cs.uc4.hyperledger.api.model

import io.jsonwebtoken.Jwts
import play.api.libs.json.{ Format, Json }

import java.util.Base64

case class SignedProposal(unsignedProposalJwt: String, signature: String) {

  def unsignedProposalAsByteArray: Array[Byte] = {
    val claims = Jwts.parser().parseClaimsJws(unsignedProposalJwt).getBody
    Base64.getDecoder.decode(claims.get("unsignedBytes", classOf[String]))
  }

  def signatureAsByteArray: Array[Byte] = Base64.getDecoder.decode(signature)
}

object SignedProposal {
  implicit val format: Format[SignedProposal] = Json.format
}
