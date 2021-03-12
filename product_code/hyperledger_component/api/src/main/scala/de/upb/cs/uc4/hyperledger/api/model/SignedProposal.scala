package de.upb.cs.uc4.hyperledger.api.model

import de.upb.cs.uc4.hyperledger.api.JWTClaimParser
import play.api.libs.json.{ Format, Json }

import java.util.Base64

case class SignedProposal(unsignedProposalJwt: String, signature: String) {

  lazy val unsignedProposalAsByteArray: Array[Byte] =
    Base64.getDecoder.decode(JWTClaimParser.readClaim("unsignedBytes", unsignedProposalJwt))

  def signatureAsByteArray: Array[Byte] = Base64.getDecoder.decode(signature)
}

object SignedProposal {
  implicit val format: Format[SignedProposal] = Json.format
}
