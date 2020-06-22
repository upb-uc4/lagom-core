package de.upb.cd.uc4.hyperledger

import java.nio.charset.StandardCharsets

import org.hyperledger.fabric.gateway.{Contract, Gateway}

class ChaincodeConnection(parameters : (Gateway, Contract)) extends AutoCloseable {

  val gateway : Gateway = parameters._1
  val chaincode : Contract = parameters._2

  @throws[Exception]
  def getCourses() : String = {
    val result = chaincode.evaluateTransaction("getAllCourses")
    return new String(result, StandardCharsets.UTF_8)
  }

  override def close() = {
    if(gateway != null) gateway.close()
  }

  def using[B](resource: ChaincodeConnection, block: ChaincodeConnection => B): B =
    try block(resource) finally resource.close()
}
