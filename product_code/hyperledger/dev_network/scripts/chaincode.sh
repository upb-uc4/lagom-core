#!/bin/bash

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

echo "############################################################################################"
echo "#                                   STARTING CHAINCODE                                     #"
echo "############################################################################################"

CORE_CHAINCODE_ID_NAME=mycc:0 CORE_PEER_TLS_ENABLED=false /opt/gopath/src/gradlew run
#CORE_CHAINCODE_ID_NAME=mycc:0 CORE_PEER_TLS_ENABLED=false ./gradlew run


popd
