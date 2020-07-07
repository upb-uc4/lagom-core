##!/bin/bash
echo "##########################"
echo "#   Starting Cassandra   #"
echo "##########################"
kubectl apply -f ".\\cassandra-service.yaml"
kubectl apply -f ".\\cassandra-statefulset.yaml"
kubectl wait pods/cassandra-0 --for=condition=Ready --timeout=300s

echo "##########################"
echo "#     Starting Kafka     #"
echo "##########################"
kubectl create namespace kafka
kubectl apply -f "https://strimzi.io/install/latest?namespace=kafka" -n kafka
kubectl apply -f ".\\kafka-single.yaml" -n kafka
kubectl wait kafka/my-cluster --for=condition=Ready --timeout=300s -n kafka

echo "##########################"
echo "#        Set RBAC        #"
echo "##########################"
kubectl apply -f ".\\rbac.yaml"

echo "##########################"
echo "#   Starting Services    #"
echo "##########################"