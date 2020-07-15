#!/bin/bash
echo "##############################"
echo "#      Starting Traefik      #"
echo "##############################"
kubectl apply -f traefik/ingress-route.yaml
kubectl apply -f traefik/traefik-service.yaml
kubectl apply -f traefik/traefik-deployment.yaml
kubectl apply -f traefik/traefik-router.yaml

echo
echo "##############################"
echo "#     Starting Postgres      #"
echo "##############################"
kubectl apply -f postgres/postgres-configmap.yaml
kubectl apply -f postgres/postgres-storage.yaml
kubectl apply -f postgres/postgres-deployment.yaml
kubectl apply -f postgres/postgres-service.yaml

echo
echo "##############################"
echo "#       Starting Kafka       #"
echo "##############################"
kubectl create namespace kafka
kubectl apply -f kafka/kafka.yaml  -n kafka
kubectl apply -f kafka/kafka-single.yaml  -n kafka

echo
echo "##############################"
echo "#        Init Postgres       #"
echo "##############################"
sleep 20
./init_postgres.sh

echo
echo "##############################"
echo "#       Wait for Kafka       #"
echo "##############################"
kubectl wait kafka/strimzi --for=condition=Ready --timeout=300s  -n kafka

echo
echo "##############################"
echo "#          Set RBAC          #"
echo "##############################"
kubectl apply -f rbac.yaml

echo
echo "##############################"
echo "#     Starting Services      #"
echo "##############################"
kubectl create secret generic user-application-secret --from-literal=secret="test"
kubectl create secret generic postgres-user --from-literal=username="uc4user" --from-literal=password="uc4user"
kubectl apply -f services/user.yaml

kubectl create secret generic authentication-application-secret --from-literal=secret="test"
kubectl create secret generic postgres-authentication --from-literal=username="uc4authentication" --from-literal=password="uc4authentication"
kubectl apply -f services/authentication.yaml

kubectl create secret generic course-application-secret --from-literal=secret="test"
kubectl create secret generic postgres-course --from-literal=username="uc4course" --from-literal=password="uc4course"
kubectl apply -f services/course.yaml

kubectl create secret generic hyperledger-application-secret --from-literal=secret="test"
#kubectl apply -f services/hyperledger.yaml

echo
echo "##############################"
echo "#       Forward Ports        #"
echo "##############################"
kubectl port-forward --address 0.0.0.0 service/traefik 9000:8000 9080:8080 9443:4443 -n default