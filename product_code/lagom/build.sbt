import scala.concurrent.duration._
import com.typesafe.sbt.packager.docker.DockerChmodType

organization in ThisBuild := "de.upb.cs.uc4"
version in ThisBuild := "0.0.2"
lagomCassandraMaxBootWaitingTime in ThisBuild := 60.seconds
lagomServiceEnableSsl in ThisBuild := true

// the Scala version that will be used for cross-compiled libraries
scalaVersion in ThisBuild := "2.13.0"

// Docker
def dockerSettings = Seq(
  dockerUpdateLatest := true,
  dockerBaseImage := "adoptopenjdk/openjdk8",
  dockerUsername := sys.props.get("docker.username"),
  dockerRepository := sys.props.get("docker.registry"),
  dockerChmodType := DockerChmodType.UserGroupWriteExecute
)

// Dependencies
val macwire = "com.softwaremill.macwire" %% "macros" % "2.3.3" % "provided"
val scalaTest = "org.scalatest" %% "scalatest" % "3.1.1" % Test
val guava = "com.google.guava" % "guava" % "29.0-jre"
val akkaDiscoveryKubernetes = "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % "1.0.8"

val apiDefaultDependencies = Seq(
  lagomScaladslApi
)

val implDefaultDependencies = Seq(
  lagomScaladslTestKit,
  lagomScaladslAkkaDiscovery,
  akkaDiscoveryKubernetes,
  filters,
  macwire,
  scalaTest
)

val defaultCassandraKafkaDependencies = Seq(
  lagomScaladslPersistenceCassandra,
  lagomScaladslKafkaBroker
)


// Projects
lazy val `lagom` = (project in file("."))
  .aggregate(`shared`,
    `course-api`, `course`,
    `hyperledger-api`, `hyperledger`,
    `authentication-api`, `authentication`)

lazy val `shared` = (project in file("shared"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslServer,
      lagomScaladslTestKit,
      scalaTest,
      filters,
      guava
    )
  )
  .dependsOn(`authentication-api`)

lazy val `hyperledger-api` = (project in file("hyperledger_service/api"))
  .settings(
    libraryDependencies ++= apiDefaultDependencies
  )

lazy val `hyperledger` = (project in file("hyperledger_service/impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= implDefaultDependencies
  )
  .settings(dockerSettings)
  .dependsOn(`hyperledger-api`, `shared`)

lazy val `course-api` = (project in file("course_service/api"))
  .settings(
    libraryDependencies ++= apiDefaultDependencies
  )

lazy val `course` = (project in file("course_service/impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= implDefaultDependencies,
    libraryDependencies ++= defaultCassandraKafkaDependencies
  )
  .settings(dockerSettings)
  .dependsOn(`course-api`, `shared`, `hyperledger-api`)

lazy val `authentication-api` = (project in file("authentication_service/api"))
  .settings(
    libraryDependencies ++= apiDefaultDependencies
  )
  .dependsOn(`user-api`)

lazy val `authentication` = (project in file("authentication_service/impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= implDefaultDependencies,
    libraryDependencies ++= defaultCassandraKafkaDependencies
  )
  .settings(dockerSettings)
  .dependsOn(`authentication-api`,  `shared`)

lazy val `user-api` = (project in file("user_service/api"))
  .settings(
    libraryDependencies ++= apiDefaultDependencies
  )