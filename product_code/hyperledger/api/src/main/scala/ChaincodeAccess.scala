package de.upb.cs.uc4.hyperledger.access

import java.nio.charset.StandardCharsets

import de.upb.cs.uc4.hyperledger.ConnectionManager
import org.hyperledger.fabric.gateway.Contract

import scala.util.Using

object ChaincodeAccess {

  @throws[Exception]
  def getCourses() : String = {
    /*
    var readable_result = null;
    Using(ConnectionManager.initializeConnection()) { (gateway, chaincode : Contract) =>
      val result = chaincode.evaluateTransaction("getAllCourses")
      readable_result = new String(result, StandardCharsets.UTF_8)
    }
    return  readable_result

    Using.Manager { use =>
      val (gateway, chaincode)  = use(ConnectionManager.initializeConnection())
    }*/

    val (gateway, chaincode) = ConnectionManager.initializeConnection()
    var readableResult : String = null;
    try {
      val result = chaincode.evaluateTransaction("getAllCourses")
      readableResult = new String(result, StandardCharsets.UTF_8)
    } finally {
      ConnectionManager.disposeGateway(gateway)
    }
    return readableResult
  }

}
