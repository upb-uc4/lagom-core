#!/bin/bash


#clone dev network

if [ ! -d ./hyperledger_chaincode/ ]
then
	git clone https://github.com/upb-uc4/hyperledger_dev_network.git
else
	read -p "Update existing dev network? " -n 1 -r
	if [[ $REPLY =~ ^[Yy]$ ]]
	then
		pushd ./dev_network
		git pull
		popd
	fi
fi

echo "#############################################"
echo "#         dev network up to date            #"
echo "#############################################"
