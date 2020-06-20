##!/bin/bash

CONTAINER_IDS=$(docker ps -a | awk '($2 ~ /fabric-peer.*/) {print $1}')
CONTAINER_IDS="$CONTAINER_IDS $(docker ps -a | awk '($2 ~ /fabric-couchdb.*/) {print $1}')"
CONTAINER_IDS="$CONTAINER_IDS $(docker ps -a | awk '($2 ~ /fabric-orderer.*/) {print $1}')"
CONTAINER_IDS="$CONTAINER_IDS $(docker ps -a | awk '($2 ~ /chaincode.*/) {print $1}')"
CONTAINER_IDS="$CONTAINER_IDS $(docker ps -a | awk '($2 ~ /fabric-tools.*/) {print $1}')"

docker rm $CONTAINER_IDS

docker-compose -f docker-compose-simple.yaml up
