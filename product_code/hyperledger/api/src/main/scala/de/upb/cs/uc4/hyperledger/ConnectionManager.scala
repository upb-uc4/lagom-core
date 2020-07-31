package de.upb.cs.uc4.hyperledger

import java.nio.file.Path

import de.upb.cs.uc4.hyperledger.traits.{ChaincodeActionsTrait, ConnectionManagerTrait}
import org.hyperledger.fabric.gateway.Gateway.Builder
import org.hyperledger.fabric.gateway._

/**
  * Manager to engage in communication with the HyperledgerNetwork
  * @param connection_profile_path Path to connectionProfile.yaml
  * @param wallet_path Path to wallet dictionary containing all certificates
  */
case class ConnectionManager(connection_profile_path : Path, wallet_path : Path)
  extends ConnectionManagerTrait{

  val channel_name = "myc"
  private val chaincode_name = "mycc"
  private val client_name = "cli"
  private val contract_name_course = "UC4.course"
  private val contract_name_student = "UC4.student"

  override def createConnection() : ChaincodeActionsTrait =
  {
    val (gateway : Gateway, contract_course : Contract, contract_student : Contract) = this.initializeConnection()
    new ChaincodeConnection(gateway, contract_course, contract_student)
  }

  @throws[Exception]
  def initializeConnection() : (Gateway, Contract, Contract) = { // Load a file system based wallet for managing identities.
    println("Try to get connection with: " + connection_profile_path + "    and: " + wallet_path)

    // retrieve possible identities
    val wallet : Wallet = this.getWallet()

    // prepare Network Builder
    val builder : Builder = this.getBuilder(wallet)

    val gateway : Gateway = builder.connect
    var contract_course : Contract = null
    var contract_student : Contract = null
    try{
      val network : Network = gateway.getNetwork(this.channel_name)
      contract_course = network.getContract(this.chaincode_name, this.contract_name_course)
      contract_student = network.getContract(this.chaincode_name, this.contract_name_student)
    } catch {
      case e : GatewayRuntimeException => this.disposeGateway(gateway); throw e;
    }

    return (gateway, contract_course, contract_student)
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
