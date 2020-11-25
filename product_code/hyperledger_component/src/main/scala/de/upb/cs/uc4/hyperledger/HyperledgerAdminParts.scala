package de.upb.cs.uc4.hyperledger

trait HyperledgerAdminParts extends HyperledgerDefaultParts {

  protected val adminUsername: String = retrieveString("uc4.hyperledger.username", "test-admin")
  protected val adminPassword: String = retrieveString("uc4.hyperledger.password", "test-admin-pw")
  protected val organisationName: String = retrieveString("uc4.hyperledger.organisationName", "org1")
  protected val organisationId: String = retrieveString("uc4.hyperledger.organisationId", "org1MSP")
}
