package de.upb.cs.uc4.hyperledger.exceptions

case class TransactionErrorException(transactionId : String, errorCode : Integer, errorDetail : String) extends Exception{

  override def toString() : String = {
    "The provided transaction: \"" + transactionId + "\" failed with an error: " + errorCode + " : " + errorDetail
  }
}
