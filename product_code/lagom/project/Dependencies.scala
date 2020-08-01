import com.lightbend.lagom.sbt.LagomImport._
import play.sbt.PlayImport.filters
import sbt._

object Dependencies {

  // libraries
  private val scalaTest = "org.scalatest" %% "scalatest" % "3.2.0" % Test
  private val macwire = "com.softwaremill.macwire" %% "macros" % "2.3.3" % "provided"
  private val guava = "com.google.guava" % "guava" % "29.0-jre"
  private val akkaDiscoveryKubernetes = "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % "1.0.8"
  private val postgresDriver = "org.postgresql" % "postgresql" % "42.2.8"
  private val uuid = "com.fasterxml.uuid" % "java-uuid-generator" % "3.1.0"
  private val janino = "org.codehaus.janino" % "janino" % "2.5.16"

  // dependency groups
  val apiDefaultDependencies = Seq(lagomScaladslApi)
  val implDefaultDependencies = Seq(
    lagomScaladslTestKit,
    lagomScaladslAkkaDiscovery,
    akkaDiscoveryKubernetes,
    filters,
    macwire,
    scalaTest,
    janino
  )
  val defaultPersistenceKafkaDependencies = Seq(
    lagomScaladslPersistenceJdbc,
    postgresDriver,
    lagomScaladslKafkaBroker
  )
  val sharedServerDependencies = Seq(
    lagomScaladslServer,
    lagomScaladslTestKit,
    scalaTest,
    filters,
    guava,
    macwire
  )
  val scalaTestDependencies = Seq(scalaTest)
  val uuidDependencies = Seq(uuid)
}