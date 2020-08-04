package de.upb.cs.uc4.hyperledger

import de.upb.cs.uc4.hyperledger.traits.ChaincodeActionsTrait
import org.hyperledger.fabric.gateway.{Contract, Gateway}

protected case class ChaincodeConnection(gateway: Gateway, contract_course: Contract, contract_student: Contract)
  extends ChaincodeActionsTrait {

  /**
   * Disposes of the network connection
   */
  override def close() = {
    if (gateway != null) gateway.close()
  }
}
