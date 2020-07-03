package de.upb.cs.uc4.hyperledger

import java.nio.charset.StandardCharsets

import de.upb.cs.uc4.hyperledger.traits.ChaincodeTrait
import org.hyperledger.fabric.gateway.{Contract, Gateway}


protected class ChaincodeConnection(parameters : (Gateway, Contract)) extends ChaincodeTrait {

  val gateway : Gateway = parameters._1
  val chaincode : Contract = parameters._2


  @throws[Exception]
  override def submitTransaction(transactionId : String, params: String*) : String = {
    val result = chaincode.submitTransaction(transactionId, params:_*)
    return new String(result, StandardCharsets.UTF_8)
  }

  @throws[Exception]
  override def evaluateTransaction(transactionId : String, params: String*) : String = {
    val result = chaincode.evaluateTransaction(transactionId, params:_*)
    return new String(result, StandardCharsets.UTF_8)
  }
  /**
   * Disposes of the network connection
   */
  override def close() = {
    if(gateway != null) gateway.close()
  }
}
