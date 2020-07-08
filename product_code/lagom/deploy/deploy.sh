##!/bin/bash
echo "##############################"
echo "#      Starting Traefik      #"
echo "##############################"
kubectl apply -f traefik/ingress-route.yaml
kubectl apply -f traefik/traefik-service.yaml
kubectl apply -f traefik/traefik-deployment.yaml
kubectl apply -f traefik/traefik-router.yaml

echo "##############################"
echo "#     Starting Cassandra     #"
echo "##############################"
kubectl apply -f cassandra-service.yaml
kubectl apply -f cassandra-statefulset.yaml

echo "##############################"
echo "#       Starting Kafka       #"
echo "##############################"
kubectl create namespace kafka
kubectl apply -f kafka.yaml  -n kafka
kubectl apply -f kafka-single.yaml  -n kafka

echo "##############################"
echo "# Wait for Kafka & Cassandra #"
echo "##############################"
kubectl wait pods/cassandra-0 --for=condition=Ready --timeout=300s
kubectl wait kafka/strimzi --for=condition=Ready --timeout=300s  -n kafka

echo "##############################"
echo "#          Set RBAC          #"
echo "##############################"
kubectl apply -f rbac.yaml

echo "##############################"
echo "#     Starting Services      #"
echo "##############################"
kubectl create secret generic user-application-secret --from-literal=secret="test"
kubectl apply -f services/user.yaml
kubectl create secret generic authentication-application-secret --from-literal=secret="test"
kubectl apply -f services/authentication.yaml
kubectl create secret generic course-application-secret --from-literal=secret="test"
kubectl apply -f services/course.yaml
kubectl create secret generic hyperledger-application-secret --from-literal=secret="test"
kubectl apply -f services/hyperledger.yaml