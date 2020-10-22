#!/bin/bash

if [ $# -eq 0 ]
  then
	echo "No cluster specified given."
    exit -1
fi

pushd ./lagom-core/deployment
./start_cluster.sh $1
popd

pushd ./hlf-network
if [ $# -eq 2 ]
then
	./deploy.sh -c /data/$1/hyperledger/ -b $2
else
	./deploy.sh -c /data/$1/hyperledger/
fi
popd

pushd ./lagom-core/deployment
./deploy_on_cluster.sh
popd