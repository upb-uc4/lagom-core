package de.upb.cd.uc4.hyperledger

trait ChaincodeActionsTrait {

  /**
   * Executes the "addCourse" query.
   * @param jSonCourse Information about the course to add.
   * @throws Exception if chaincode throws an exception.
   * @return Success_state
   */
  @throws[Exception]
  def addCourse(jSonCourse : String) : String

  /**
   * Executes the "getCourses" query.
   * @throws Exception if chaincode throws an exception.
   * @return List of courses represented by their json value.
   */
  @throws[Exception]
  def getAllCourses() : String

  /**
   * Executes the "getCourseById" query.
   * @param courseId courseId to get course information
   * @throws Exception if chaincode throws an exception.
   * @return JSon Course Object
   */
  @throws[Exception]
  def getCourseById(courseId : String) : String

  /**
   * Submits the "deleteCourseById" query.
   * @param courseId courseId to delete course
   * @throws Exception if chaincode throws an exception.
   * @return success_state
   */
  @throws[Exception]
  def deleteCourseById(courseId : String) : String

  /**
   * Submits the "updateCourseById" query.
   * @param courseId courseId to update course
   * @param jSonCourse courseInfo to update to
   * @throws Exception if chaincode throws an exception.
   * @return success_state
   */
  @throws[Exception]
  def updateCourseById(courseId : String, jSonCourse : String) : String
}
