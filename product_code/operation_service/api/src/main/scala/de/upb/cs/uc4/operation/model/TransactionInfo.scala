package de.upb.cs.uc4.operation.model

import play.api.libs.json.{ Format, Json }

case class TransactionInfo(contractName: String, transactionName: String, parameters: Seq[String])

object TransactionInfo {
  implicit val format: Format[TransactionInfo] = Json.format
}
