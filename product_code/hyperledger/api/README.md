# Scala Hyperleger API

## Prerequesites

1. Have a working UC4-chaincode-network running 
    (download from [dev_network](https://github.com/upb-uc4/University-Credits-4.0/tree/develop/product_code/hyperledger/dev_network)
    and  [chaincode](https://github.com/upb-uc4/University-Credits-4.0/tree/develop/product_code/hyperledger/chaincode)
    )
2. Store the .yaml describing the network (example provided in ./connection_profile.yaml)
3. Store the wallet-directory containing the certificate (example provided in ./wallet/cli.id)

## Configuration / Initialization

1. Add the dependencies to your scala project
```
lazy val yourProject = (project in file("."))
  .dependsOn(hyperledger_api)

lazy val hyperledger_api = ProjectRef(file("<Path to dir>/hyperledger/api"), "api")
```
2. Import the Manager and the ConnectionObject Traits and Classes
```
import de.upb.cs.uc4.hyperledger.traits.{ChaincodeTrait, ConnectionManagerTrait}
import de.upb.cs.uc4.hyperledger.ConnectionManager
```

## Communicate with the Network

1. Configure a manager by initializing with your wallet and network_description
```
lazy val connectionManager: ConnectionManagerTrait = ConnectionManager(
    Paths.get(getClass.getResource("/connection_profile.yaml").toURI),
    Paths.get(getClass.getResource("/wallet/").toURI)
  )
```

2. Engage in Communication by starting a connection
```
val chaincodeConnection: ChaincodeTrait = connectionManager.createConnection()
```

3. Pass commands to the Connection
```
try {
    val result = chaincodeConnection.evaluateTransaction(transactionId, params: _*))
} catch {
    case e: Exception => HandleError(e)
}
```