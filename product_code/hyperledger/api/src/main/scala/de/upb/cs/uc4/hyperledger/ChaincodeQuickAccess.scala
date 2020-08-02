package de.upb.cs.uc4.hyperledger

import de.upb.cs.uc4.hyperledger.traits.{ChaincodeActionsTrait, ChaincodeTrait}

import scala.util.Using

object ChaincodeQuickAccess extends ChaincodeActionsTrait {

  val connectionManager = ConnectionManager()

  @throws[Exception]
  override def internalSubmitTransaction(transactionId : String, params : String*) : Array[Byte] = Using(connectionManager.createConnection()) { chaincodeConnection: ChaincodeTrait =>
    return chaincodeConnection.internalSubmitTransaction(transactionId, params:_*)
  }.get

  @throws[Exception]
  override def internalEvaluateTransaction(transactionId : String, params : String*) : Array[Byte] = Using(connectionManager.createConnection()) { chaincodeConnection: ChaincodeTrait =>
    return chaincodeConnection.internalEvaluateTransaction(transactionId, params:_*)
  }.get
}
