package de.upb.cs.uc4.hyperledger.exceptions

class TransactionErrorException(transactionId : String, errorCode : Integer, errorDetail : String) extends Exception{

  override def toString() : String = {
    "The provided transaction: \"" + transactionId + "\" failed with an error: " + errorCode + " : " + errorDetail
  }

  def getErrorCode : Integer = errorCode
  def getErrorDetail : String = errorDetail
  def getTransactionId : String = transactionId
}
