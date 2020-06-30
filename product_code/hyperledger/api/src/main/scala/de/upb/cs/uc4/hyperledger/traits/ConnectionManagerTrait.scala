package de.upb.cs.uc4.hyperledger.traits

/**
 * Trait to provide access to the [[de.upb.cs.uc4.hyperledger.ConnectionManager]]
 */
trait ConnectionManagerTrait {
  /**
   * Creates a connection object
   * @return returns the connection object created.
   */
  def createConnection() : ChaincodeTrait
}
