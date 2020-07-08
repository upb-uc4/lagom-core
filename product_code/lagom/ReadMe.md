# How to build

#### Changes necessary on UNIX
Add in .sbtopts '-J' in front of every parameter.
Should look like this:
```
-J-Xms512M
-J-Xmx4096M
-J-Xss2M
-J-XX:MaxMetaspaceSize=1024M
```

#### Run
Use following command:
```shell script
sbt runAll
````
If you don't have sbt installed, go home.

#### Errors
If you encounter any unexpected errors, use following command:
````shell script
sbt clean
````
Attention: This resets the Cassandra Database!

If this doesn't help: run in circles, screaming,
and call the (in)famous LAGOM TEAM.


# How to deploy

Install any kind of kubernetes. We are using minikube
in a docker container.
(Important: Set docker memory limit high enough)

\<name\> represents the name of a single service.

#### Create Docker Images
````sbtshell
sbt <name>_service/docker:publish
````

#### Starting minikube:
````shell script
minikube start --memory='10g' --cpus=4
```` 

#### Starting Cassandra:
Every YAML file is in /deploy.
````shell script
kubectl apply -f .\cassandra-service.yaml
kubectl apply -f .\cassandra-statefulset.yaml  
kubectl wait pods/cassandra-0 --for=condition=Ready --timeout=300s
````

#### Starting Kafka:
````shell script
kubectl create namespace kafka
kubectl apply -f 'https://strimzi.io/install/latest?namespace=kafka' -n kafka
kubectl apply -f .\kafka-single.yaml -n kafka
kubectl wait kafka/strimzi --for=condition=Ready --timeout=300s -n kafka
````

#### Setting Role Based Access Control
````shell script
kubectl apply -f .\rbac.yaml 
````

#### Starting the different services
````shell script
kubectl create secret generic <name>-application-secret --from-literal=secret="test"
kubectl apply -f <name>.yaml
````

#### Accessing a service
Because we are using kubernetes in docker container, we
need to open a tunnel to access a service.
````shell script
minikube service <name>
````
````shell script
kubectl port-forward --address 0.0.0.0 service/traefik 8000:8000 8080:8080 443:4443 -n default
````