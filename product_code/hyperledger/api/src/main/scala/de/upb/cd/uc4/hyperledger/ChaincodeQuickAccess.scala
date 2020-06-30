package de.upb.cd.uc4.hyperledger

import scala.util.{Success, Try, Using}

object ChaincodeQuickAccess extends ChaincodeActionsTrait {

  @throws[Exception]
  override def getAllCourses() : String = Using(ConnectionManager.createConnection()) { chaincodeConnection: ChaincodeConnection =>
    return chaincodeConnection.getAllCourses()
  }.get

  @throws[Exception]
  override def addCourse(jSonCourse : String) : String = Using(ConnectionManager.createConnection()) { chaincodeConnection: ChaincodeConnection =>
    return chaincodeConnection.addCourse(jSonCourse)
  }.get

  @throws[Exception]
  def getCourseById(courseId : String) : String = Using(ConnectionManager.createConnection()) { chaincodeConnection: ChaincodeConnection =>
    return chaincodeConnection.getCourseById(courseId)
  }.get

  @throws[Exception]
  def deleteCourseById(courseId : String) : String = Using(ConnectionManager.createConnection()) { chaincodeConnection: ChaincodeConnection =>
    return chaincodeConnection.deleteCourseById(courseId)
  }.get

  @throws[Exception]
  def updateCourseById(courseId : String, jSonCourse : String) : String = Using(ConnectionManager.createConnection()) { chaincodeConnection: ChaincodeConnection =>
    return chaincodeConnection.updateCourseById(courseId, jSonCourse)
  }.get
}
