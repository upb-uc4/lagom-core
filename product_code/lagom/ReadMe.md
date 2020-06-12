# How to build

### Changes necessary on UNIX
Add in .sbtopts '-J' in front of every parameter.
Should look like this:
```
-J-Xms512M
-J-Xmx4096M
-J-Xss2M
-J-XX:MaxMetaspaceSize=1024M
```

### Run
Use following command:
```shell script
sbt runAll
````
If you don't have sbt installed, go home.

### Errors
If you encounter any unexpected errors, use following command:
````shell script
sbt clean
````
Attention: This resets the Cassandra Database!

If this doesn't help: run in circles, screaming,
and call the (in)famous LAGOM TEAM.