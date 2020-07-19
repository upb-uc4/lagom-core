package de.upb.cs.uc4.hyperledger.traits

import java.nio.charset.StandardCharsets

/**
 * Trait to provide explicit access to chaincode transactions.
 */
trait ChaincodeActionsTrait {

  /**
   * Executes the "addCourse" query.
   * @param jSonCourse Information about the course to add.
   * @throws Exception if chaincode throws an exception.
   * @return Success_state
   */
  @throws[Exception]
  final def addCourse(jSonCourse : String) : String = {
    wrapSubmitTransactionResult("addCourse", this.internalSubmitTransaction("addCourse", jSonCourse))
  }

  /**
   * Submits the "deleteCourseById" query.
   * @param courseId courseId to delete course
   * @throws Exception if chaincode throws an exception.
   * @return success_state
   */
  @throws[Exception]
  final def deleteCourseById(courseId : String) : String = {
    wrapSubmitTransactionResult("deleteCourseById", this.internalSubmitTransaction("deleteCourseById", courseId))
  }

  /**
   * Submits the "updateCourseById" query.
   * @param courseId courseId to update course
   * @param jSonCourse courseInfo to update to
   * @throws Exception if chaincode throws an exception.
   * @return success_state
   */
  @throws[Exception]
  final def updateCourseById(courseId : String, jSonCourse : String) : String = {
    wrapSubmitTransactionResult("updateCourseById", this.internalSubmitTransaction("updateCourseById", courseId, jSonCourse))
  }

  /**
   * Executes the "getCourses" query.
   * @throws Exception if chaincode throws an exception.
   * @return List of courses represented by their json value.
   */
  @throws[Exception]
  final def getAllCourses() : String = {
    val result = wrapEvaluateTransactionResult("getAllCourses", this.internalEvaluateTransaction("getAllCourses"))

    // check specific error
    if (!result.startsWith("[") || !result.endsWith("]")) throw new Exception("Something went wrong on getAllCourses. received result : " + result)
    else return result
  }

  /**
   * Executes the "getCourseById" query.
   * @param courseId courseId to get course information
   * @throws Exception if chaincode throws an exception.
   * @return JSon Course Object
   */
  @throws[Exception]
  final def getCourseById(courseId : String) : String = {
    val result = wrapEvaluateTransactionResult("getCourseById", this.internalEvaluateTransaction("getCourseById", courseId))

    // check specific error
    if (result == "null") throw new Exception("Something went wrong on getCourseById. received result : " + result)
    else return result
  }

  /**
   * Submits any transaction specified by transactionId.
   * @param transactionId transactionId to submit
   * @param params parameters to pass to the transaction
   * @throws Exception if chaincode throws an exception.
   * @return success_state
   */
  @throws[Exception]
  final def submitTransaction(transactionId : String, params : String*) : String = {
    transactionId match {
      case "addCourse" => this.addCourse(params.apply(0))
      case "deleteCourseById" => this.deleteCourseById(params.apply(0))
      case "updateCourseById" => this.updateCourseById(params.apply(0), params.apply(1))
      case _ => throw new Exception("Illegal transactionId:: \"" + transactionId + "\".")
    }
  }

  /**
   * Evaluates the transaction specified by transactionId.
   * @param transactionId transactionId to evaluate
   * @param params parameters to pass to the transaction
   * @throws Exception if chaincode throws an exception.
   * @return success_state
   */
  @throws[Exception]
  final def evaluateTransaction(transactionId : String, params : String*) : String = {
    transactionId match {
      case "getCourseById" => this.getCourseById(params.apply(0))
      case "getAllCourses" => this.getAllCourses()
      case _ => throw new Exception("Illegal transactionId:: \"" + transactionId + "\".")
    }
  }

  /**
   * Submits any transaction specified by transactionId.
   * @param transactionId transactionId to submit
   * @param params parameters to pass to the transaction
   * @throws Exception if chaincode throws an exception.
   * @return success_state
   */
  @throws[Exception]
  protected def internalSubmitTransaction(transactionId : String, params : String*) : Array[Byte]

  /**
   * Evaluates the transaction specified by transactionId.
   * @param transactionId transactionId to evaluate
   * @param params parameters to pass to the transaction
   * @throws Exception if chaincode throws an exception.
   * @return success_state
   */
  @throws[Exception]
  protected def internalEvaluateTransaction(transactionId : String, params : String*) : Array[Byte]

  /**
   * Wraps the chaincode query result bytes.
   * Translates the byte-array to a string and throws an error if said string is not empty
   * @param result inbut byte-array to translate
   * @return result as a string
   */
  private def wrapEvaluateTransactionResult(transactionId : String, result : Array[Byte]) : String = {
    new String(result, StandardCharsets.UTF_8)
  }

  /**
   * Wraps the chaincode query result bytes.
   * Translates the byte-array to a string and throws an error if said string is not empty
   * @param result inbut byte-array to translate
   * @return result as a string
   */
  protected def wrapSubmitTransactionResult(transactionId : String, result : Array[Byte]) : String = {
    val resultString = new String(result, StandardCharsets.UTF_8)
    if (!resultString.equals("")) throw new Exception("Something went wrong on " + transactionId + ". Received result : " + result)
    else return resultString
  }
}
