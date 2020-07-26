package de.upb.cs.uc4.hyperledger.exceptions

class InvalidTransactionException(transactionId : String) extends Exception{

  override def toString() : String = {
    "The provided transaction: \"" + transactionId + "\" was not valid."
  }

}
