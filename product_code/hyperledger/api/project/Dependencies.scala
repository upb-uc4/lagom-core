import sbt._

object Dependencies {
  // libraries
  private val hyperledger_sdk = "org.hyperledger.fabric-sdk-java" % "fabric-sdk-java" % "2.1.1"
  private val hyperledger_gateway = "org.hyperledger.fabric" % "fabric-gateway-java" % "2.1.1"
  private val scalaTest = "org.scalatest" %% "scalatest" % "3.2.0" % Test

  // dependency groups
  val hyperledgerDependencies = Seq(hyperledger_sdk, hyperledger_gateway)
  val scalaTestDependencies = Seq(scalaTest)
}