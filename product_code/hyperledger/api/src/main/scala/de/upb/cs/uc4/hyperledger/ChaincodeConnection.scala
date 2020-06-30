package de.upb.cs.uc4.hyperledger

import java.nio.charset.StandardCharsets

import de.upb.cs.uc4.hyperledger.traits.ChaincodeTrait
import org.hyperledger.fabric.gateway.{Contract, Gateway}

protected class ChaincodeConnection(parameters : (Gateway, Contract)) extends ChaincodeTrait {

  val gateway : Gateway = parameters._1
  val chaincode : Contract = parameters._2

  @throws[Exception]
  override def addCourse(jSonCourse : String) : String = {
    val result = chaincode.submitTransaction("addCourse", jSonCourse)
    return new String(result, StandardCharsets.UTF_8)
  }

  @throws[Exception]
  override def getAllCourses() : String = {
    val result = chaincode.evaluateTransaction("getAllCourses")
    return new String(result, StandardCharsets.UTF_8)
  }

  @throws[Exception]
  override def getCourseById(courseId : String) : String = {
    val result = chaincode.evaluateTransaction("getCourseById", courseId)
    return new String(result, StandardCharsets.UTF_8)
  }

  @throws[Exception]
  override def deleteCourseById(courseId : String) : String = {
    val result = chaincode.submitTransaction("deleteCourseById", courseId)
    return new String(result, StandardCharsets.UTF_8)
  }

  @throws[Exception]
  override def updateCourseById(courseId : String, jSonCourse : String) : String = {
    val result = chaincode.submitTransaction("updateCourseById", courseId, jSonCourse )
    return new String(result, StandardCharsets.UTF_8)
  }

  /**
   * Disposes of the network connection
   */
  override def close() = {
    if(gateway != null) gateway.close()
  }
}
