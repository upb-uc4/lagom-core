#!/bin/bash
echo
echo "##############################"
echo "#      Starting Traefik      #"
echo "##############################"
kubectl apply -f traefik/ingress-route.yaml
kubectl apply -f traefik/traefik-service.yaml
kubectl apply -f traefik/traefik-deployment.yaml
kubectl apply -f traefik/traefik-router.yaml

echo
echo "##############################"
echo "#      Starting Support      #"
echo "##############################"
kubectl create namespace uc4-support
kubectl apply -f support/imaginary.yaml

echo
echo "##############################"
echo "#     Starting Postgres      #"
echo "##############################"
kubectl create namespace postgres
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
sleep 10
kubectl apply -f kafka/kafka-single.yaml  -n kafka

echo
echo "##############################"
echo "# Wait for Kafka & Postgres  #"
echo "##############################"
kubectl wait --for=condition=Ready pods --all --timeout=300s -n postgres
kubectl wait kafka/strimzi --for=condition=Ready --timeout=300s  -n kafka
sleep 60

echo
echo "##############################"
echo "#          Set RBAC          #"
echo "##############################"
kubectl create namespace uc4-lagom
kubectl apply -f rbac.yaml

echo
echo "##############################"
echo "#     Starting Services      #"
echo "##############################"
kubectl create secret generic application-secret --from-literal=secret="$(openssl rand -base64 48)" -n uc4-lagom

kubectl apply -f secrets/user.yaml
kubectl apply -f services/user.yaml

kubectl apply -f secrets/authentication.yaml
kubectl apply -f services/authentication.yaml

kubectl apply -f secrets/course.yaml
kubectl apply -f services/course.yaml

kubectl apply -f services/matriculation.yaml

kubectl apply -f secrets/certificate.yaml
kubectl apply -f services/certificate.yaml

kubectl apply -f services/configuration.yaml