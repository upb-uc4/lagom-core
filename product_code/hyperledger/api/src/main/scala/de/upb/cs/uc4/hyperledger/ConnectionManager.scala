package de.upb.cs.uc4.hyperledger

import java.nio.file.{Path, Paths}

import de.upb.cs.uc4.hyperledger.traits.{ChaincodeTrait, ConnectionManagerTrait}
import org.hyperledger.fabric.gateway.Gateway.Builder
import org.hyperledger.fabric.gateway._

object ConnectionManager extends ConnectionManagerTrait{

  val channel_name = "myc"
  private val chaincode_name = "mycc"
  private val client_name = "cli"
  private val connection_profile_path = Paths.get("connection_profile.yaml")
  private val walletPath = Paths.get("wallet")

  override def createConnection() : ChaincodeTrait = { new ChaincodeConnection(ConnectionManager.initializeConnection()) }

  @throws[Exception]
  def initializeConnection() : (Gateway, Contract) = { // Load a file system based wallet for managing identities.

    // retrieve possible identities
    val wallet : Wallet = ConnectionManager.getWallet()

    // prepare Network Builder
    val builder : Builder = ConnectionManager.getBuilder(wallet)

    val gateway : Gateway = builder.connect
    var contract : Contract = null
    try{
      val network : Network = gateway.getNetwork(ConnectionManager.channel_name)
      contract = network.getContract(ConnectionManager.chaincode_name)
    } catch {
      case e : GatewayRuntimeException => ConnectionManager.disposeGateway(gateway); throw e;
    }

    return (gateway, contract)
  }

  def disposeGateway(gateway: Gateway) = {
    if(gateway != null) gateway.close()
  }

  def getBuilder(wallet : Wallet,
                 networkConfigPath : Path = ConnectionManager.connection_profile_path,
                 name : String = ConnectionManager.client_name) : Builder = {
    // load a CCP
    val builder = Gateway.createBuilder
    builder.identity(wallet, name).networkConfig(networkConfigPath).discovery(true)
    return builder
  }

  def getWallet(): Wallet = {
    val wallet = Wallets.newFileSystemWallet(walletPath)
    return wallet
  }

  System.setProperty("org.hyperledger.fabric.sdk.service_discovery.as_localhost", "true")
}
