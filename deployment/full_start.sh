#!/bin/bash

if [ $# -eq 0 ]
  then
	echo "No cluster specified given."
    exit -1
fi

./start_cluster.sh $1
./deploy_on_cluster.sh