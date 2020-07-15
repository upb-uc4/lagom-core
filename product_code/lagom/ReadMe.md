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
First start Postgres:
```shell script
docker-compose up
```
Attention, this occupies your shell.  
You may want to detach with 'ctrl + d' or with '-d' option. 

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

#### Deploy everything
````shell script
./deploy/deploy.sh
```` 