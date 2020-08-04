package de.upb.cs.uc4.hyperledger.exceptions

case class HyperledgerInnerException(transactionId: String, innerException: Exception) extends Exception {

  override def toString(): String = {
    "The provided transaction: \"" + transactionId + "\" failed with internal Hyperledger exception:\n" + innerException
  }
}
