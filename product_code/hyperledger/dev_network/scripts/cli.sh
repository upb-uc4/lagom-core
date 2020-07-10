#!/bin/bash

sleep 10s 

source /opt/gopath/src/chaincodedev/scripts/variables.sh

echo "############################################################################################"
echo "#                                   SETTING UP CHANNEL                                     #"
echo "############################################################################################"

set -e
peer channel create -c myc -f myc.tx -o orderer:7050
peer channel join -b myc.block

echo "############################################################################################"
echo "#                                   CHANNEL SETUP DONE                                     #"
echo "############################################################################################"

# wait for 'chaincode' to compile chaincode
echo "" | nc -l -p 8080
echo "Continuing..."
# wait for 'chaincode' to start chaincode
sleep 10s

#/opt/gopath/src/chaincodedev

echo "############################################################################################"
echo "#                                   INSTALLING CHAINCODE                                   #"
echo "############################################################################################"

# chaincode points to the chaincode directory in the UC4 repo
peer chaincode install -p chaincode -n ${CHAINCODE_NAME} -v 0 -l java

echo "############################################################################################"
echo "#                                   CHAINCODE INSTALLED                                    #"
echo "############################################################################################"

echo "############################################################################################"
echo "#                                   INITIALIZING CHAINCODE                                 #"
echo "############################################################################################"

# TODO: change the function to call the actual init-function
peer chaincode instantiate -n ${CHAINCODE_NAME} -v 0 -c '{"Args":["initLedger"]}' -C myc

echo "############################################################################################"
echo "#                                   CHAINCODE INITIALIZED                                  #"
echo "#                                     READY FOR ACTION                                     #"
echo "############################################################################################"

sleep 600000
