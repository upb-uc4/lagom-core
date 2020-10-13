package de.upb.cs.uc4.shared.client.exceptions

import de.upb.cs.uc4.shared.client.exceptions.ErrorType.ErrorType
import play.api.libs.json._

case class TransactionError(`type`: ErrorType, title: String, transactionId: String) extends UC4Error

object TransactionError {
  implicit val format: Format[TransactionError] = Json.format

  def apply(`type`: ErrorType, transactionId: String): TransactionError = {
    val title = ErrorType.getTitle(`type`)
    new TransactionError(`type`, title, transactionId)
  }
}

