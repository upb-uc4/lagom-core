package de.upb.cs.uc4.hyperledger

import de.upb.cs.uc4.hyperledger.traits.{ChaincodeActionsTrait, ChaincodeTrait}

import scala.util.Using

object ChaincodeQuickAccess extends ChaincodeActionsTrait {

  @throws[Exception]
  override def submitTransaction(transactionId : String, params : String*) : String = Using(ConnectionManager.createConnection()) { chaincodeConnection: ChaincodeTrait =>
    return chaincodeConnection.submitTransaction(transactionId, params:_*)
  }.get

  @throws[Exception]
  override def evaluateTransaction(transactionId : String, params : String*) : String = Using(ConnectionManager.createConnection()) { chaincodeConnection: ChaincodeTrait =>
    return chaincodeConnection.evaluateTransaction(transactionId, params:_*)
  }.get
}
