#!/bin/bash

set -e
peer channel create -c myc -f myc.tx -o orderer:7050
peer channel join -b myc.block

# waiting for chaincode to be compiled and started
echo "Start sleep"
sleep 60s
echo "Finish sleep"

#/opt/gopath/src/chaincodedev

# chaincode points to the chaincode directory in the UC4 repo
peer chaincode install -p chaincode -n mycc -v 0 -l java

echo "Chaincode installed"

# TODO: change the function to call the actual init-function
peer chaincode instantiate -n mycc -v 0 -c '{"Args":["initLedger"]}' -C myc

sleep 600000
