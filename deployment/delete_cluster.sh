#!/bin/bash

if [ $# -eq 0 ]
  then
	echo "No cluster specified given."
    exit -1
fi

echo
echo "================================================================================"
echo "Delete Cluster"
echo "================================================================================"

kind delete cluster --name $1
rm "/data/$1" -r