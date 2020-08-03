package de.upb.cs.uc4.hyperledger.traits

import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeoutException

import de.upb.cs.uc4.hyperledger.exceptions.{HyperledgerInnerException, InvalidCallException, TransactionErrorException, UnhandledException}
import org.hyperledger.fabric.gateway.{Contract, ContractException, GatewayRuntimeException}

/**
 * Trait to provide basic functionality for all chaincode transactions.
 */
protected trait ChaincodeActionsTraitInternal extends AutoCloseable {

  @throws[Exception]
  protected final def internalSubmitTransaction(chaincode: Contract, transactionId: String, params: String*): Array[Byte] = {
    try{
      chaincode.submitTransaction(transactionId, params: _*)
    } catch {
      case ex : ContractException => throw HyperledgerInnerException(transactionId, ex)
      case ex : TimeoutException =>throw HyperledgerInnerException(transactionId, ex)
      case ex : java.lang.InterruptedException =>throw HyperledgerInnerException(transactionId, ex)
      case ex : GatewayRuntimeException => throw HyperledgerInnerException(transactionId, ex)
      case ex : Exception => throw UnhandledException(transactionId, ex)
    }
  }

  @throws[Exception]
  protected final def internalEvaluateTransaction(chaincode: Contract, transactionId: String, params: String*): Array[Byte] = {
    try{
      chaincode.evaluateTransaction(transactionId, params: _*)
    } catch {
      case ex : ContractException => throw HyperledgerInnerException(transactionId, ex)
      case ex : Exception => throw UnhandledException(transactionId, ex)
    }
  }

  /**
   * Wraps the chaincode query result bytes.
   * Translates the byte-array to a string and throws an error if said string is not empty
   *
   * @param result inbut byte-array to translate
   * @return result as a string
   */
  protected def wrapTransactionResult(transactionId: String, result: Array[Byte]): String = {
    val resultString = convertTransactionResult(result)
    if (containsError(resultString)) throw extractErrorFromResult(transactionId, resultString)
    else return resultString
  }

  protected def extractErrorFromResult(transactionId: String, result: String): TransactionErrorException = {
    // retrieve error code
    var id = result.substring(result.indexOf("\"name\":\"") + 8)
    id = id.substring(0, id.indexOf("\""))

    // retrieve detail
    var detail = result.substring(result.indexOf("\"detail\":\"") + 10)
    detail = detail.substring(0, detail.indexOf("\""))

    // create Exception
    TransactionErrorException(transactionId, Integer.parseInt(id), detail)
  }

  /**
   * Evaluates whether a transaction was valid or invalid
   *
   * @param result result of a chaincode transaction
   * @return true if the result contains error information
   */
  private def containsError(result: String): Boolean = {
    result.contains("{\"name\":") && result.contains("\"detail\":")
  }

  /**
   * Since the chain returns bytes, we need to convert them to a readable Result.
   *
   * @param result Bytes containing a result from a chaincode transaction.
   * @return Result as a String.
   */
  private def convertTransactionResult(result: Array[Byte]): String = {
    new String(result, StandardCharsets.UTF_8)
  }

  protected def validateParameterCount(transactionId: String, expected: Integer, params: Array[String]) = {
    if (params.size != expected) throw InvalidCallException.CreateInvalidParameterCountException(transactionId, expected, params.size)
  }
}
