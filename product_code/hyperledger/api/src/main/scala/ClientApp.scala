import java.nio.charset.StandardCharsets

import org.hyperledger.fabric.gateway.Gateway
import org.hyperledger.fabric.gateway.Wallets
import java.nio.file.Paths

object ClientApp {
  @throws[Exception]
  def main(args: Array[String]) = { // Load a file system based wallet for managing identities.
    val walletPath = Paths.get("wallet")
    val wallet = Wallets.newFileSystemWallet(walletPath)
    // load a CCP
    val networkConfigPath = Paths.get("..", "..", "..", "test-network", "organizations", "peerOrganizations", "org1.example.com", "connection-org1.yaml")
    val builder = Gateway.createBuilder
    builder.identity(wallet, "appUser").networkConfig(networkConfigPath).discovery(true)
    // create a gateway connection
    try {
      val gateway = builder.connect
      try { // get the network and contract
        println("try get network")
        val network = gateway.getNetwork("myc")
        println("try get contract uc4")
        val contract = network.getContract("UC4")
        println("try queryALl")
        var result = contract.evaluateTransaction("queryAll")
        println("queryResult: ")
        println(new String(result, StandardCharsets.UTF_8))
        println("Begin transaction:")
        contract.submitTransaction("changeLectureId", "FoC", "Foundations of Cryptography")
        result = contract.evaluateTransaction("queryAll")
        println("After transaction:")
        println(new String(result, StandardCharsets.UTF_8))
      } finally if (gateway != null) gateway.close()
    }
  }

  try System.setProperty("org.hyperledger.fabric.sdk.service_discovery.as_localhost", "true")
}