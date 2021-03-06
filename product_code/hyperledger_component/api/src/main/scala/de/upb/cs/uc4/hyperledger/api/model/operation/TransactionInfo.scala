package de.upb.cs.uc4.hyperledger.api.model.operation

import play.api.libs.json.{ Format, Json }

case class TransactionInfo(contractName: String, transactionName: String, parameters: String)

object TransactionInfo {
  implicit val format: Format[TransactionInfo] = Json.format
}
