package de.upb.cs.uc4.hyperledger

import java.nio.charset.StandardCharsets

import de.upb.cs.uc4.hyperledger.traits.ChaincodeTrait
import org.hyperledger.fabric.gateway.{Contract, Gateway}


protected class ChaincodeConnection(parameters : (Gateway, Contract)) extends ChaincodeTrait {

  val gateway : Gateway = parameters._1
  val chaincode : Contract = parameters._2

  @throws[Exception]
  override def internalSubmitTransaction(transactionId : String, params: String*) : Array[Byte] = {
    chaincode.submitTransaction(transactionId, params:_*)
  }

  @throws[Exception]
  override def internalEvaluateTransaction(transactionId : String, params: String*) : Array[Byte] = {
    chaincode.evaluateTransaction(transactionId, params:_*)
  }

  /**
   * Disposes of the network connection
   */
  override def close() = {
    if(gateway != null) gateway.close()
  }
}
