package de.upb.cs.uc4.hyperledger

import java.nio.file.{Path, Paths}

import de.upb.cs.uc4.hyperledger.traits.{ChaincodeTrait, ConnectionManagerTrait}
import org.hyperledger.fabric.gateway.Gateway.Builder
import org.hyperledger.fabric.gateway._

case class ConnectionManager(
    connection_profile_path : Path = Paths.get("connection_profile.yaml"),
    wallet_path : Path = Paths.get("wallet"))
      extends ConnectionManagerTrait{

  val channel_name = "myc"
  private val chaincode_name = "mycc"
  private val client_name = "cli"

  override def createConnection() : ChaincodeTrait = { new ChaincodeConnection(this.initializeConnection()) }

  @throws[Exception]
  def initializeConnection() : (Gateway, Contract) = { // Load a file system based wallet for managing identities.
    println("Try to get connection with: " + connection_profile_path + "    and: " + wallet_path)

    // retrieve possible identities
    val wallet : Wallet = this.getWallet()

    // prepare Network Builder
    val builder : Builder = this.getBuilder(wallet)

    val gateway : Gateway = builder.connect
    var contract : Contract = null
    try{
      val network : Network = gateway.getNetwork(this.channel_name)
      contract = network.getContract(this.chaincode_name)
    } catch {
      case e : GatewayRuntimeException => this.disposeGateway(gateway); throw e;
    }

    return (gateway, contract)
  }

  def disposeGateway(gateway: Gateway) = {
    if(gateway != null) gateway.close()
  }

  def getBuilder(wallet : Wallet,
                 networkConfigPath : Path = this.connection_profile_path,
                 name : String = this.client_name) : Builder = {
    // load a CCP
    val builder = Gateway.createBuilder
    builder.identity(wallet, name).networkConfig(networkConfigPath).discovery(true)
    return builder
  }

  def getWallet(): Wallet = {
    val wallet = Wallets.newFileSystemWallet(this.wallet_path)
    return wallet
  }

  System.setProperty("org.hyperledger.fabric.sdk.service_discovery.as_localhost", "true")
}
