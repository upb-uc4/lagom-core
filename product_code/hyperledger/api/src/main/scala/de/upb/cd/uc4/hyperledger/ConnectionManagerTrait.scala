package de.upb.cd.uc4.hyperledger

trait ConnectionManagerTrait {
  /**
   * Creates a connection object
   * @return returns the connection object created.
   */
  def createConnection() : ChaincodeConnection
}
