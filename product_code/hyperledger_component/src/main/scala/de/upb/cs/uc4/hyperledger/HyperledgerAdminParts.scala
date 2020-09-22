package de.upb.cs.uc4.hyperledger

trait HyperledgerAdminParts extends HyperledgerDefaultParts {

  protected val username: String = retrieveString("uc4.hyperledger.username")
  protected val password: String = retrieveString("uc4.hyperledger.password")
  protected val organisationId: String = retrieveString("uc4.hyperledger.organisationId")
  protected val organisationName: String = retrieveString("uc4.hyperledger.organisationId")
}
