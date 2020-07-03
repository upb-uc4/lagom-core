package de.upb.cs.uc4.hyperledger

import de.upb.cs.uc4.hyperledger.traits.{ChaincodeActionsTrait, ChaincodeTrait}

import scala.util.Using

object ChaincodeQuickAccess extends ChaincodeActionsTrait {

  @throws[Exception]
  override def getAllCourses() : String = Using(ConnectionManager.createConnection()) { chaincodeConnection: ChaincodeTrait =>
    return chaincodeConnection.getAllCourses()
  }.get

  @throws[Exception]
  override def addCourse(jSonCourse : String) : String = Using(ConnectionManager.createConnection()) { chaincodeConnection: ChaincodeTrait =>
    return chaincodeConnection.addCourse(jSonCourse)
  }.get

  @throws[Exception]
  def getCourseById(courseId : String) : String = Using(ConnectionManager.createConnection()) { chaincodeConnection: ChaincodeTrait =>
    return chaincodeConnection.getCourseById(courseId)
  }.get

  @throws[Exception]
  def deleteCourseById(courseId : String) : String = Using(ConnectionManager.createConnection()) { chaincodeConnection: ChaincodeTrait =>
    return chaincodeConnection.deleteCourseById(courseId)
  }.get

  @throws[Exception]
  def updateCourseById(courseId : String, jSonCourse : String) : String = Using(ConnectionManager.createConnection()) { chaincodeConnection: ChaincodeTrait =>
    return chaincodeConnection.updateCourseById(courseId, jSonCourse)
  }.get
}
