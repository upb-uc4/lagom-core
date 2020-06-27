package de.upb.cd.uc4.hyperledger

import java.nio.charset.StandardCharsets

import org.hyperledger.fabric.gateway.{Contract, Gateway}

class ChaincodeConnection(parameters : (Gateway, Contract)) extends AutoCloseable {

  val gateway : Gateway = parameters._1
  val chaincode : Contract = parameters._2

  /**
   * Executes the "addCourse" query.
   * @param jSonCourse Information about the course to add.
   * @throws
   * @return Success_state
   */
  @throws[Exception]
  def addCourse(jSonCourse : String) : String = {
    val result = chaincode.submitTransaction("addCourse", jSonCourse)
    return new String(result, StandardCharsets.UTF_8)
  }

  /**
   * Executes the "getCourses" query.
   * @throws
   * @return List of courses represented by their json value.
   */
  @throws[Exception]
  def getAllCourses() : String = {
    val result = chaincode.evaluateTransaction("getAllCourses")
    return new String(result, StandardCharsets.UTF_8)
  }

  /**
   * Executes the "getCourseById" query.
   * @param courseId courseId to get course information
   * @throws
   * @return JSon Course Object
   */
  @throws[Exception]
  def getCourseById(courseId : String) : String = {
    val result = chaincode.evaluateTransaction("getCourseById", courseId)
    return new String(result, StandardCharsets.UTF_8)
  }

  /**
   * Submits the "deleteCourseById" query.
   * @param courseId courseId to delete course
   * @throws
   * @return success_state
   */
  @throws[Exception]
  def deleteCourseById(courseId : String) : String = {
    val result = chaincode.submitTransaction("deleteCourseById", courseId)
    return new String(result, StandardCharsets.UTF_8)
  }

  /**
   * Submits the "updateCourseById" query.
   * @param courseId courseId to update course
   * @param jSonCourse courseInfo to update to
   * @throws
   * @return success_state
   */
  @throws[Exception]
  def updateCourseById(courseId : String, jSonCourse : String) : String = {
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
