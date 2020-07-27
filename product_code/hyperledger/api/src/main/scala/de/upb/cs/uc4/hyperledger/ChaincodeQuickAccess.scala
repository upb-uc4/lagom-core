package de.upb.cs.uc4.hyperledger

import java.nio.file.Paths

import de.upb.cs.uc4.hyperledger.traits.ChaincodeActionsTrait

import scala.util.Using

object ChaincodeQuickAccess extends ChaincodeActionsTrait {

  val connectionManager = ConnectionManager(
    Paths.get(getClass.getResource("/connection_profile.yaml").toURI),
    Paths.get(getClass.getResource("/wallet/").toURI)
  )

  @throws[Exception]
  override def internalSubmitTransaction(transactionId : String, params : String*) : Array[Byte] = Using(connectionManager.createConnection()) { chaincodeConnection: ChaincodeActionsTrait =>
    return chaincodeConnection.internalSubmitTransaction(transactionId, params:_*)
  }.get

  @throws[Exception]
  override def internalEvaluateTransaction(transactionId : String, params : String*) : Array[Byte] = Using(connectionManager.createConnection()) { chaincodeConnection: ChaincodeActionsTrait =>
    return chaincodeConnection.internalEvaluateTransaction(transactionId, params:_*)
  }.get

  override def close(): Unit = throw new NotImplementedError("No need to close QuickAccess.")
}
