package de.upb.cd.uc4.hyperledger

import scala.util.{Success, Try, Using}

object ChaincodeQuickAccess {

  @throws[Exception]
  def getAllCourses() : Try[String] = Using(ConnectionManager.createConnection()) { chaincodeConnection: ChaincodeConnection =>
    return Success(chaincodeConnection.getAllCourses())
  }
  @throws[Exception]
  def addCourse(jSonCourse : String) : Try[String] = Using(ConnectionManager.createConnection()) { chaincodeConnection: ChaincodeConnection =>
    return Success(chaincodeConnection.addCourse(jSonCourse))
  }
  @throws[Exception]
  def getCourseById(courseId : String) : Try[String] = Using(ConnectionManager.createConnection()) { chaincodeConnection: ChaincodeConnection =>
    return Success(chaincodeConnection.getCourseById(courseId))
  }
  @throws[Exception]
  def deleteCourseById(courseId : String) : Try[String] = Using(ConnectionManager.createConnection()) { chaincodeConnection: ChaincodeConnection =>
    return Success(chaincodeConnection.deleteCourseById(courseId))
  }
  @throws[Exception]
  def updateCourseById(courseId : String, jSonCourse : String) : Try[String] = Using(ConnectionManager.createConnection()) { chaincodeConnection: ChaincodeConnection =>
    return Success(chaincodeConnection.updateCourseById(courseId, jSonCourse))
  }

}
