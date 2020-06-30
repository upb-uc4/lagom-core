package de.upb.cs.uc4.hyperledger.traits

/**
 * Trait to provide access to [[de.upb.cs.uc4.hyperledger.ChaincodeConnection]] (closable and chaincode actions)
 */
trait ChaincodeTrait extends ChaincodeActionsTrait with AutoCloseable{

}
