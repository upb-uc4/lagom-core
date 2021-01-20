package de.upb.cs.uc4.hyperledger.api.model

import play.api.libs.json.{ Format, Json }

import java.util.Base64

/** The default response for a unsigned transaction
  *
  * @param unsignedProposal the base64 encoded proposal
  */
case class UnsignedProposal(unsignedProposal: String)

object UnsignedProposal {

  /** Creates a [[UnsignedProposal]] out of a byte array
    *
    * @param unsignedProposal byte array which gets base64 encoded
    * @return base64 encoded proposal
    */
  def apply(unsignedProposal: Array[Byte]) = new UnsignedProposal(Base64.getEncoder.encodeToString(unsignedProposal))

  implicit val format: Format[UnsignedProposal] = Json.format
}