package de.upb.cs.uc4.hyperledger.api.model

import de.upb.cs.uc4.hyperledger.api.JWTClaimParser
import play.api.libs.json.{ Format, Json }

/** The default response for a unsigned transaction
  *
  * @param unsignedTransactionJwt the jwt that contains the unsignedTransaction
  */
case class UnsignedTransaction(unsignedTransactionJwt: String) {

  lazy val unsignedTransaction: String =
    JWTClaimParser.readClaim("unsignedBytes", unsignedTransactionJwt)
}

object UnsignedTransaction {
  implicit val format: Format[UnsignedTransaction] = Json.format
}
