package de.upb.cs.uc4.shared.client

import java.util.Base64
import play.api.libs.json.{ Format, Json }

/** The default response for a unsigned transaction
  *
  * @param unsignedProposal the base64 encoded proposal
  */
case class UnsignedProposal(unsignedProposal: String)

object UnsignedProposal {

  /** Creates a [[de.upb.cs.uc4.shared.client.UnsignedProposal]] out of a byte array
    *
    * @param unsignedProposal byte array which gets base64 encoded
    * @return base64 encoded proposal
    */
  def apply(unsignedProposal: Array[Byte]) = new UnsignedProposal(Base64.getEncoder.encodeToString(unsignedProposal))

  implicit val format: Format[UnsignedProposal] = Json.format
}