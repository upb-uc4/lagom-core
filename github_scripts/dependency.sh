#!/bin/sh
  
service=${{ github.event.inputs.target_service }}
if sbt "dependencyCheck $service"
then
  echo "Failure=0" >> $GITHUB_ENV
else
  echo "Failure=1" >> $GITHUB_ENV
  message=`cat target/dependencyCheck.txt`
  echo "FailureMessage=$message" >> $GITHUB_ENV
  echo "ServiceName=$service" >> $GITHUB_ENV
fi