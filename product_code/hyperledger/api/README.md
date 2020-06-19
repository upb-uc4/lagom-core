# Scala Chaincode Invocation Example

## Instructions

### Prerequesites

Follow the instructions of these tutorials to install the required tools and the hyperledger sample network:
1. https://hyperledger-fabric.readthedocs.io/en/release-2.1/prereqs.html
2. https://hyperledger-fabric.readthedocs.io/en/release-2.1/install.html

Make sure you work on release 2.1.

### Setup

Then, move to the folder `fabric_samples/chaincode/` and execute the following command to clone this project:
 
`git clone <git-url> UC4`

Replace the file `deployCC.sh` in the folder `fabric_samples/first-network/scripts/` by the file in this directory.
Run `chmod 755 deployCC.sh` to make the file executable.

### Run the program

To start the network and deploy or chaincode execute the `startFabric.sh` file.
Run `sbt test` to run our test cases and access the chaincode.

Use `networkDown.sh` to shutdown and reset the network. 

