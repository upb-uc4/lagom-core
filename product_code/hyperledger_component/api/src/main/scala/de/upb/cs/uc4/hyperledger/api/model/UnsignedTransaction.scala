package de.upb.cs.uc4.hyperledger.api.model

import play.api.libs.json.{ Format, Json }

/** The default response for a unsigned transaction
  *
  * @param unsignedTransaction the base64 encoded transaction
  */
case class UnsignedTransaction(unsignedTransactionJwt: String)

object UnsignedTransaction {
  implicit val format: Format[UnsignedTransaction] = Json.format
}
