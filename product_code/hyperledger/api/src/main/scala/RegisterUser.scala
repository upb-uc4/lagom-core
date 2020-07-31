import org.hyperledger.fabric.gateway.{Identities, Identity, Wallet, Wallets, X509Identity}
import org.hyperledger.fabric.sdk.Enrollment
import org.hyperledger.fabric.sdk.User
import org.hyperledger.fabric.sdk.security.CryptoSuiteFactory
import org.hyperledger.fabric_ca.sdk.HFCAClient
import org.hyperledger.fabric_ca.sdk.RegistrationRequest
import java.nio.file.Paths
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.Properties

object RegisterUser {
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
    // Check to see if we've already enrolled the user.
    var userExists = wallet.list.contains("appUser")
    if (userExists) {
      println("An identity for the user \"appUser\" already exists in the wallet")
      return
    }
    userExists = wallet.list.contains("admin")
    if (!userExists) {
      System.out.println("\"admin\" needs to be enrolled and added to the wallet first")
      return
    }
    val adminIdentity: X509Identity = wallet.get("admin").asInstanceOf[X509Identity]
    val admin = new User() {
      override def getName = "admin"

      override def getRoles = null

      override def getAccount = null

      override def getAffiliation = "org1.department1"

      override def getEnrollment = new Enrollment() {
        override def getKey: PrivateKey = adminIdentity.getPrivateKey

        override def getCert: String = Identities.toPemString(adminIdentity.getCertificate)
      }

      override def getMspId = "Org1MSP"
    }
    println(adminIdentity.getCertificate.asInstanceOf[X509Certificate].toString)
    // Register the user, enroll the user, and import the new identity into the wallet.
    val registrationRequest = new RegistrationRequest("appUser")
    registrationRequest.setAffiliation("org1.department1")
    registrationRequest.setEnrollmentID("appUser")
    val enrollmentSecret = caClient.register(registrationRequest, admin)
    val enrollment = caClient.enroll("appUser", enrollmentSecret)
    val user = Identities.newX509Identity("Org1MSP", enrollment)
    wallet.put("appUser", user)
    println("Successfully enrolled user \"appUser\" and imported it into the wallet")
  }

  System.setProperty("org.hyperledger.fabric.sdk.service_discovery.as_localhost", "true")
}