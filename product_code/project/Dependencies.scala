import com.lightbend.lagom.sbt.LagomImport._
import play.sbt.PlayImport.filters
import sbt._

object Dependencies {
  val macwire = "com.softwaremill.macwire" %% "macros" % "2.3.3" % "provided"
  val scalaTest = "org.scalatest" %% "scalatest" % "3.2.0" % Test
  val flexmark = "com.vladsch.flexmark" % "flexmark-all" % "0.35.10" % Test
  val guava = "com.google.guava" % "guava" % "29.0-jre"
  val akkaDiscoveryKubernetes = "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % "1.0.8"
  val postgresDriver = "org.postgresql" % "postgresql" % "42.2.8"
  val uuid = "com.fasterxml.uuid" % "java-uuid-generator" % "3.1.0"
  val janino = "org.codehaus.janino" % "janino" % "2.5.16"
  val hyperledger_api = RootProject(uri("https://github.com/upb-uc4/hlf-api.git#%s".format(Version("hyperledger_api"))))

  val apiDefaultDependencies = Seq(
    lagomScaladslApi,
    scalaTest,
    flexmark
  )

  val implDefaultDependencies = Seq(
    lagomScaladslTestKit,
    lagomScaladslAkkaDiscovery,
    akkaDiscoveryKubernetes,
    filters,
    macwire,
    scalaTest,
    flexmark,
    janino
  )

  val defaultPersistenceKafkaDependencies = Seq(
    lagomScaladslPersistenceJdbc,
    postgresDriver,
    lagomScaladslKafkaBroker
  )
}
