package de.upb.cs.uc4.hyperledger.api.model

import de.upb.cs.uc4.hyperledger.api.JWTClaimParser
import play.api.libs.json.{ Format, Json }

/** The default response for a unsigned transaction
  *
  * @param unsignedProposalJwt the jwt that contains the unsignedProposal
  */
case class UnsignedProposal(unsignedProposalJwt: String) {

  lazy val unsignedProposal: String =
    JWTClaimParser.readClaim("unsignedBytes", unsignedProposalJwt)
}

object UnsignedProposal {
  implicit val format: Format[UnsignedProposal] = Json.format
}