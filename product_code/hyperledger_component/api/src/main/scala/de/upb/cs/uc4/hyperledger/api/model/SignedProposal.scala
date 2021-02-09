package de.upb.cs.uc4.hyperledger.api.model

import play.api.libs.json.{ Format, Json }

import java.util.Base64

case class SignedProposal(unsignedProposal: String, signature: String) {

  def unsignedProposalAsByteArray: Array[Byte] = Base64.getDecoder.decode(unsignedProposal)

  def signatureAsByteArray: Array[Byte] = Base64.getDecoder.decode(signature)
}

object SignedProposal {
  implicit val format: Format[SignedProposal] = Json.format
}