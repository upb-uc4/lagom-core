package de.upb.cs.uc4.shared.client

import java.util.Base64
import play.api.libs.json.{ Format, Json }

/** The default response for a unsigned transaction
  *
  * @param unsignedProposal the base64 encoded proposal
  */
case class TransactionProposal(unsignedProposal: String)

object TransactionProposal {

  /** Creates a [[de.upb.cs.uc4.shared.client.TransactionProposal]] out of a byte array
    *
    * @param unsignedProposal byte array which gets base64 encoded
    * @return base64 encoded proposal
    */
  def apply(unsignedProposal: Array[Byte]) = new TransactionProposal(Base64.getEncoder.encodeToString(unsignedProposal))

  implicit val format: Format[TransactionProposal] = Json.format
}