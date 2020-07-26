package de.upb.cs.uc4.hyperledger.exceptions

class TransactionErrorException(transactionId : String, errorDescription : String) extends Exception{

  override def toString() : String = {
    "The provided transaction: \"" + transactionId + "\" failed with an error: " + errorDescription
  }

}
