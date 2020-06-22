package de.upb.cd.uc4.hyperledger

import scala.util.{Success, Try, Using}

object ChaincodeQuickAccess {

  @throws[Exception]
  def getCourses() : Try[String] = Using(ConnectionManager.createConnection()) { chaincodeConnection: ChaincodeConnection =>
    return Success(chaincodeConnection.getCourses())
  }

}
