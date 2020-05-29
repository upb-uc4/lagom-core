#!/bin/bash

echo "Start sleep"
sleep 15s
echo "Finish sleep"

pushd /opt/gopath/src/chaincode

./../gradlew installDist

CORE_CHAINCODE_ID_NAME=mycc:0 CORE_PEER_TLS_ENABLED=false ./../gradlew run

popd
