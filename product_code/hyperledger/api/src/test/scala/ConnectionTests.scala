import de.upb.cs.uc4.hyperledger.ConnectionManager
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.PrivateMethodTester

class ConnectionTests extends AnyWordSpec with PrivateMethodTester {

  val connectionManager = ConnectionManager()

  "The connectionManager" when {
    "connecting to Chain" should {

      "provide gateway connection" in {
        // retrieve possible identities
        val wallet = connectionManager.getWallet()
        assert(wallet != null, "Wallet retrieved was null.")

        // prepare Network Builder
        val builder = connectionManager.getBuilder(wallet)
        assert(builder != null, "Builder retrieved was null, maybe the connection profile did not match the running network.")

        // get gateway object
        val gateway = builder.connect
        assert(gateway != null, "Gateway retrieved was null.")

        // cleanup
        connectionManager.disposeGateway(gateway)
      }

      "provide network connection" in {
        // retrieve possible identities
        val wallet = connectionManager.getWallet()
        assert(wallet != null, "Wallet retrieved was null.")

        // prepare Network Builder
        val builder = connectionManager.getBuilder(wallet)
        assert(builder != null, "Builder retrieved was null, maybe the connection profile did not match the running network.")

        // get gateway object
        val gateway = builder.connect
        assert(gateway != null, "Gateway retrieved was null.")

        // try connecting to the network
        try{
          val network = gateway.getNetwork(connectionManager.channel_name)
          assert(network != null, "Network retrieved was null.")
        } finally {
          connectionManager.disposeGateway(gateway)
        }
      }

      "Provice chaincode connection" in {
        // test full chaincode connection
        val (gateway, chaincode) = connectionManager.initializeConnection()
        assert(gateway != null, "Gateway retrieved was null.")
        assert(chaincode != null, "Chaincode retrieved was null.")

        // cleanup
        connectionManager.disposeGateway(gateway)
      }
    }
  }
}
