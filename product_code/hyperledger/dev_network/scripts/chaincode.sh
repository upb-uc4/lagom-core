#!/bin/bash

source /opt/gopath/src/scripts/variables.sh

echo "Start sleep"
sleep 15s
echo "Finish sleep"

pushd /opt/gopath/src/chaincode

echo "############################################################################################"
echo "#                                   COMPILING CHAINCODE                                    #"
echo "############################################################################################"

/opt/gopath/src/gradlew installDist
#./gradlew installDist

echo "############################################################################################"
echo "#                                   CHAINCODE COMPILED                                     #"
echo "############################################################################################"

# notify 'cli' that chaincode is compiled
echo "Continued cli..." | nc cli 8080
echo "Continuing..."

echo "############################################################################################"
echo "#                                   STARTING CHAINCODE                                     #"
echo "############################################################################################"

CORE_CHAINCODE_ID_NAME=${CHAINCODE_NAME}:0 CORE_PEER_TLS_ENABLED=false /opt/gopath/src/gradlew run
#CORE_CHAINCODE_ID_NAME=mycc:0 CORE_PEER_TLS_ENABLED=false ./gradlew run


popd
