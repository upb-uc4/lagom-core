package de.upb.cs.uc4.hyperledger.exceptions

case class InvalidCallException(transactionId : String, detail : String) extends Exception{

  override def toString() : String = {
    "The transaction: \"" + this.transactionId + "\" could not be invoked.\n" + "Details: " + this.detail
  }
}

object InvalidCallException {
  def CreateInvalidIDException(transactionId : String) : InvalidCallException = {
    new InvalidCallException(transactionId, "The transaction is not defined.")
  }
  def CreateInvalidParameterCountException(transactionId : String, expected : Integer, actual : Integer) : InvalidCallException = {
    new InvalidCallException(transactionId,
      "The transaction was invoked with the wrong amount of parameters. Expected: " + expected + " Actual: " + actual)
  }
}
