import org.hyperledger.fabric.gateway.Wallets
import org.hyperledger.fabric.gateway.Identities
import org.hyperledger.fabric.sdk.security.CryptoSuiteFactory
import org.hyperledger.fabric_ca.sdk.EnrollmentRequest
import org.hyperledger.fabric_ca.sdk.HFCAClient
import java.nio.file.Paths
import java.util.Properties

object EnrollAdmin {
  @throws[Exception]
  def main(args: Array[String]): Unit = { // Create a CA client for interacting with the CA.
    val props = new Properties
    props.put("pemFile", "../../../test-network/organizations/peerOrganizations/org1.example.com/ca/ca.org1.example.com-cert.pem")
    props.put("allowAllHostNames", "true")
    val caClient = HFCAClient.createNewInstance("https://localhost:7054", props)
    val cryptoSuite = CryptoSuiteFactory.getDefault.getCryptoSuite
    caClient.setCryptoSuite(cryptoSuite)

    // Create a wallet for managing identities
    val wallet = Wallets.newFileSystemWallet(Paths.get("wallet"))

    // Check to see if we've already enrolled the admin user.
    val adminExists = wallet.list.contains("admin")
    if (adminExists) {
      System.out.println("An identity for the admin user \"admin\" already exists in the wallet")
      return
    }
    // Enroll the admin user, and import the new identity into the wallet.
    val enrollmentRequestTLS = new EnrollmentRequest
    enrollmentRequestTLS.addHost("localhost")
    enrollmentRequestTLS.setProfile("tls")
    val enrollment = caClient.enroll("admin", "adminpw", enrollmentRequestTLS)
    val user = Identities.newX509Identity("Org1MSP", enrollment)
    wallet.put("admin", user)
    System.out.println("Successfully enrolled user \"admin\" and imported it into the wallet")
  }

  try System.setProperty("org.hyperledger.fabric.sdk.service_discovery.as_localhost", "true")
}