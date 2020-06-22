import org.scalatest.FunSuite

class ConnectionTests extends FunSuite {

  /*  Simple Test to check for an available connection to our chaincode.
   *  Will fail if any step in connecting to the chaincode fails
   */
  test("Check gateway connection") {
    // retrieve possible identities
    val wallet = ClientApp.getWallet()
    assert(wallet != null, "Wallet retrieved was null.")

    // prepare Network Builder
    val builder = ClientApp.getBuilder(wallet, ClientApp.connection_profile_path, ClientApp.client_name)
    assert(builder != null, "Builder retrieved was null, maybe the connection profile did not match the running network.")

    // get gateway object
    val gateway = builder.connect
    assert(gateway != null, "Gateway retrieved was null.")

    // cleanup
    ClientApp.disposeGateway(gateway)
  }

  /*  Simple Test to check for an available connection to our chaincode.
   *  Will fail if any step in connecting to the chaincode fails
   */
  test("Check network connection") {

    // retrieve possible identities
    val wallet = ClientApp.getWallet()
    assert(wallet != null, "Wallet retrieved was null.")

    // prepare Network Builder
    val builder = ClientApp.getBuilder(wallet, ClientApp.connection_profile_path, ClientApp.client_name)
    assert(builder != null, "Builder retrieved was null, maybe the connection profile did not match the running network.")

    // get gateway object
    val gateway = builder.connect
    assert(gateway != null, "Gateway retrieved was null.")

    // try connecting to the network
    try{
      val network = gateway.getNetwork(ClientApp.channel_name)
      assert(network != null, "Network retrieved was null.")
    } finally {
      ClientApp.disposeGateway(gateway)
    }
  }

  /*  Simple Test to check for an available connection to our chaincode.
   *  Will fail if any step in connecting to the chaincode fails
   */
  test("Check chaincode connection") {
    // test full chaincode connection
    val gateway = ClientApp.initializeConnection()
    assert(gateway != null, "Gateway retrieved was null.")

    // cleanup
    ClientApp.disposeGateway(gateway)
  }

}
