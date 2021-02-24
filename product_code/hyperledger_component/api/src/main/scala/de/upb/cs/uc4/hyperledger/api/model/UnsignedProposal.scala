package de.upb.cs.uc4.hyperledger.api.model

import io.jsonwebtoken.Jwts
import play.api.libs.json.{ Format, Json }

/** The default response for a unsigned transaction
  *
  * @param unsignedProposal the base64 encoded proposal
  */
case class UnsignedProposal(unsignedProposalJwt: String) {

  def unsignedProposal: String = {
    val claims = Jwts.parser().parseClaimsJws(unsignedProposalJwt).getBody
    claims.get("unsignedBytes", classOf[String])
  }
}

object UnsignedProposal {
  implicit val format: Format[UnsignedProposal] = Json.format
}