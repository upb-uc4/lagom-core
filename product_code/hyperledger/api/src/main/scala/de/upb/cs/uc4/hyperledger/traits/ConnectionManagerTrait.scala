package de.upb.cs.uc4.hyperledger.traits

trait ConnectionManagerTrait {
  /**
   * Creates a connection object
   * @return returns the connection object created.
   */
  def createConnection() : ChaincodeTrait
}
