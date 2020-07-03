package de.upb.cs.uc4.hyperledger.traits

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
  def addCourse(jSonCourse : String) : String = { this.submitTransaction("addCourse",jSonCourse) }

  /**
   * Executes the "getCourses" query.
   * @throws Exception if chaincode throws an exception.
   * @return List of courses represented by their json value.
   */
  @throws[Exception]
  def getAllCourses() : String = { this.evaluateTransaction("getAllCourses") }

  /**
   * Executes the "getCourseById" query.
   * @param courseId courseId to get course information
   * @throws Exception if chaincode throws an exception.
   * @return JSon Course Object
   */
  @throws[Exception]
  def getCourseById(courseId : String) : String = { this.evaluateTransaction("getCourseById", courseId) }

  /**
   * Submits the "deleteCourseById" query.
   * @param courseId courseId to delete course
   * @throws Exception if chaincode throws an exception.
   * @return success_state
   */
  @throws[Exception]
  def deleteCourseById(courseId : String) : String = { this.submitTransaction("deleteCourseById", courseId) }

  /**
   * Submits the "updateCourseById" query.
   * @param courseId courseId to update course
   * @param jSonCourse courseInfo to update to
   * @throws Exception if chaincode throws an exception.
   * @return success_state
   */
  @throws[Exception]
  def updateCourseById(courseId : String, jSonCourse : String) : String = { this.submitTransaction("updateCourseById", courseId, jSonCourse) }

  /**
   * Submits any transaction specified by transactionId.
   * @param transactionId transactionId to submit
   * @param params parameters to pass to the transaction
   * @throws Exception if chaincode throws an exception.
   * @return success_state
   */
  @throws[Exception]
  def submitTransaction(transactionId : String, params : String*) : String

  /**
   * Evaluates the transaction specified by transactionId.
   * @param transactionId transactionId to evaluate
   * @param params parameters to pass to the transaction
   * @throws Exception if chaincode throws an exception.
   * @return success_state
   */
  @throws[Exception]
  def evaluateTransaction(transactionId : String, params : String*) : String
}
