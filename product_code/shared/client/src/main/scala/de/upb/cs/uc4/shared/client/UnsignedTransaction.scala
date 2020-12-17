package de.upb.cs.uc4.shared.client

import java.util.Base64

import play.api.libs.json.{ Format, Json }

/** The default response for a unsigned transaction
  *
  * @param unsignedTransaction the base64 encoded transaction
  */
case class UnsignedTransaction(unsignedTransaction: String)

object UnsignedTransaction {

  /** Creates a [[de.upb.cs.uc4.shared.client.UnsignedTransaction]] out of a byte array
    *
    * @param unsignedTransaction byte array which gets base64 encoded
    * @return base64 encoded transaction
    */
  def apply(unsignedTransaction: Array[Byte]) = new UnsignedTransaction(Base64.getEncoder.encodeToString(unsignedTransaction))

  implicit val format: Format[UnsignedTransaction] = Json.format
}
