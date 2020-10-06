#!/bin/sh

cd product_code
if sbt "dependencyCheck $1"
then
  echo "Failure=0" >> $GITHUB_ENV
else
  echo "Failure=1" >> $GITHUB_ENV
  message=`cat target/dependencyCheck.txt`
  echo "FailureMessage=$message" >> $GITHUB_ENV
  echo "ServiceName=$1" >> $GITHUB_ENV
fi