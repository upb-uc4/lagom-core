package de.upb.cs.uc4.hyperledger.traits

import java.nio.charset.StandardCharsets

import de.upb.cs.uc4.hyperledger.exceptions.{InvalidCallException, TransactionErrorException}

/**
 * Trait to provide basic functionality for all chaincode transactions.
 */
protected trait ChaincodeActionsTraitInternal extends AutoCloseable {

  /**
   * Submits any transaction specified by transactionId.
   * @param transactionId transactionId to submit
   * @param params parameters to pass to the transaction
   * @throws Exception if chaincode throws an exception.
   * @return success_state
   */
  @throws[Exception]
  def submitTransaction(transactionId : String, params : String*) : String

  /**
   * Evaluates the transaction specified by transactionId.
   * @param transactionId transactionId to evaluate
   * @param params parameters to pass to the transaction
   * @throws Exception if chaincode throws an exception.
   * @return success_state
   */
  @throws[Exception]
  def evaluateTransaction(transactionId : String, params : String*) : String

  /**
   * Submits any transaction specified by transactionId.
   * @param transactionId transactionId to submit
   * @param params parameters to pass to the transaction
   * @throws Exception if chaincode throws an exception.
   * @return success_state
   */
  @throws[Exception]
  def internalSubmitTransaction(transactionId : String, params : String*) : Array[Byte]

  /**
   * Evaluates the transaction specified by transactionId.
   * @param transactionId transactionId to evaluate
   * @param params parameters to pass to the transaction
   * @throws Exception if chaincode throws an exception.
   * @return success_state
   */
  @throws[Exception]
  def internalEvaluateTransaction(transactionId : String, params : String*) : Array[Byte]

  /**
   * Wraps the chaincode query result bytes.
   * Translates the byte-array to a string and throws an error if said string is not empty
   * @param result inbut byte-array to translate
   * @return result as a string
   */
  protected def wrapTransactionResult(transactionId : String, result : Array[Byte]) : String = {
    val resultString = convertTransactionResult(result)
    if(containsError(resultString)) throw extractErrorFromResult(transactionId, resultString)
    else return resultString
  }

  protected def extractErrorFromResult(transactionId : String, result : String): TransactionErrorException = {
    // retrieve error code
    var id = result.substring(result.indexOf("\"name\":\"")+8)
    id = id.substring(0, id.indexOf("\""))

    // retrieve detail
    var detail = result.substring(result.indexOf("\"detail\":\"")+10)
    detail = detail.substring(0, detail.indexOf("\""))

    // create Exception
    TransactionErrorException(transactionId, Integer.parseInt(id), detail)
  }

  /**
   * Evaluates whether a transaction was valid or invalid
   * @param result result of a chaincode transaction
   * @return true if the result contains error information
   */
  private def containsError(result : String) : Boolean = {
    result.contains("{\"name\":") && result.contains("\"detail\":")
  }

  /**
   * Since the chain returns bytes, we need to convert them to a readable Result.
   * @param result Bytes containing a result from a chaincode transaction.
   * @return Result as a String.
   */
  private def convertTransactionResult(result : Array[Byte]) : String = {
    new String(result, StandardCharsets.UTF_8)
  }

  protected def validateParameterCount(transactionId : String, expected : Integer, params : Array[String]) = {
    if (params.size != expected) throw InvalidCallException.CreateInvalidParameterCountException(transactionId, expected, params.size)
  }
}
