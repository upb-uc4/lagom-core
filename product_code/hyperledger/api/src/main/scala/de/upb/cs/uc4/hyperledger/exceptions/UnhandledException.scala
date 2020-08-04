package de.upb.cs.uc4.hyperledger.exceptions

case class UnhandledException(transactionId: String, innerException: Exception) extends Exception {

  override def toString(): String = {
    "The provided transaction: \"" + transactionId + "\" failed with an unhandled exception:\n" + innerException
  }
}
