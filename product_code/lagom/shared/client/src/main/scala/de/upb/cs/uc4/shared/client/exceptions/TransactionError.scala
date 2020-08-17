package de.upb.cs.uc4.shared.client.exceptions

import play.api.libs.json._

case class TransactionError(`type`: String, title: String, transactionId: String) extends CustomError

object TransactionError {
  implicit val format: Format[TransactionError] = Json.format

  def apply(`type`: String, transactionId: String): TransactionError = {
    val title = CustomError.getTitle(`type`)
    new TransactionError(`type`, title, transactionId)
  }
}

