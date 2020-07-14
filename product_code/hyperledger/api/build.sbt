name := "hyperledger_api"
organization := "de.upb.cs.uc4"
version := "v0.3.0"

scalaVersion := "2.13.0"

libraryDependencies += "org.hyperledger.fabric-sdk-java" % "fabric-sdk-java" % "2.1.1"
libraryDependencies += "org.hyperledger.fabric" % "fabric-gateway-java" % "2.1.1"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.0" % Test
