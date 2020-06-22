import java.nio.charset.StandardCharsets

import org.hyperledger.fabric.gateway.{Contract, Gateway, Network, Wallet, Wallets}
import java.nio.file.{Path, Paths}

import org.hyperledger.fabric.gateway.Gateway.Builder

object ChaincodeAccessManager {

  val channel_name = "myc"
  val chaincode_name = "mycc"
  val client_name = "cli"
  val connection_profile_path = Paths.get("connection_profile.yaml")

  @throws[Exception]
  def main(args: Array[String]) = {
    val (gateway, chaincode) = ChaincodeAccessManager.initializeConnection()

    try{
      var result = chaincode.evaluateTransaction("queryAll")
      println("Before:")
      println(new String(result, StandardCharsets.UTF_8))
      chaincode.submitTransaction("changeLectureId", "FoC", "Foundations of Cryptography")
      result = chaincode.evaluateTransaction("queryAll")
      println("After:")
      println(new String(result, StandardCharsets.UTF_8))
    } finally {
      ChaincodeAccessManager.disposeGateway(gateway)
    }

  }

  @throws[Exception]
  def initializeConnection() : (Gateway, Contract) = { // Load a file system based wallet for managing identities.

    // retrieve possible identities
    val wallet : Wallet = ChaincodeAccessManager.getWallet()

    // prepare Network Builder
    val builder : Builder = ChaincodeAccessManager.getBuilder(wallet, ChaincodeAccessManager.connection_profile_path, ChaincodeAccessManager.client_name)

    val gateway : Gateway = builder.connect
    var contract : Contract = null
    try{
      val network : Network = gateway.getNetwork(ChaincodeAccessManager.channel_name)
      contract = network.getContract(ChaincodeAccessManager.chaincode_name)
    } catch {
      case e => ChaincodeAccessManager.disposeGateway(gateway); throw e;
    }

    return (gateway, contract)
  }

  def disposeGateway(gateway: Gateway) = {
    if(gateway != null) gateway.close()
  }

  def getBuilder(wallet : Wallet, networkConfigPath : Path, name : String) : Builder = {
    // load a CCP
    val builder = Gateway.createBuilder
    builder.identity(wallet, name).networkConfig(networkConfigPath).discovery(true)
    return builder
  }

  def getWallet(): Wallet = {
    val walletPath = Paths.get("wallet")
    val wallet = Wallets.newFileSystemWallet(walletPath)
    return wallet
  }

  try System.setProperty("org.hyperledger.fabric.sdk.service_discovery.as_localhost", "true")
}
