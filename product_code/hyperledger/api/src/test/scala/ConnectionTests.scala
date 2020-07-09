import de.upb.cs.uc4.hyperledger.ConnectionManager
import org.scalatest.FunSuite
import org.scalatest.PrivateMethodTester

class ConnectionTests extends FunSuite with PrivateMethodTester {

  val connectionManager = ConnectionManager()

  /*  Simple Test to check for an available gateway according to the network configuration file
   */
  test("Check gateway connection") {
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

  /*  Simple Test to check for an available connection to our network.
   */
  test("Check network connection") {

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

  /*  Simple Test to check for an available connection to our chaincode.
   *  Will fail if any step in connecting to the chaincode fails
   */
  test("Check chaincode connection") {
    // test full chaincode connection
    val (gateway, chaincode) = connectionManager.initializeConnection()
    assert(gateway != null, "Gateway retrieved was null.")
    assert(chaincode != null, "Chaincode retrieved was null.")

    // cleanup
    connectionManager.disposeGateway(gateway)
  }

}
