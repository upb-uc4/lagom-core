import com.typesafe.sbt.packager.docker.DockerChmodType

organization in ThisBuild := "de.upb.cs.uc4"
version in ThisBuild := "v0.4.2"
lagomServiceEnableSsl in ThisBuild := true

// The project uses PostgreSQL
lagomCassandraEnabled in ThisBuild := false

// the Scala version that will be used for cross-compiled libraries
scalaVersion in ThisBuild := "2.13.0"

// Docker
def dockerSettings = Seq(
  dockerUpdateLatest := true,
  dockerBaseImage := "adoptopenjdk/openjdk8",
  dockerUsername := Some("uc4official"),
  dockerChmodType := DockerChmodType.UserGroupWriteExecute,
)

// Dependencies
val macwire = "com.softwaremill.macwire" %% "macros" % "2.3.3" % "provided"
val scalaTest = "org.scalatest" %% "scalatest" % "3.2.0" % Test
val guava = "com.google.guava" % "guava" % "29.0-jre"
val akkaDiscoveryKubernetes = "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % "1.0.8"
val postgresDriver = "org.postgresql" % "postgresql" % "42.2.8"
val uuid = "com.fasterxml.uuid" % "java-uuid-generator" % "3.1.0"
val janino = "org.codehaus.janino" % "janino" % "2.5.16"

val apiDefaultDependencies = Seq(
  lagomScaladslApi
)

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
  lagomScaladslKafkaBroker,
)

// Projects
lazy val lagom = (project in file("."))
  .aggregate(shared_client, shared_server,
    course_service_api, course_service,
    hl_course_service_api, hl_course_service,
    hyperledger_service_api, hyperledger_service,
    authentication_service_api, authentication_service,
    user_service_api, user_service)
  .dependsOn(shared_client, shared_server,
    course_service_api, course_service,
    hl_course_service_api, hl_course_service,
    hyperledger_service_api, hyperledger_service,
    authentication_service_api, authentication_service,
    user_service_api, user_service)

// This project is not allowed to have lagom server dependencies
lazy val shared_client = (project in file("shared/client"))
  .settings(
    libraryDependencies ++= apiDefaultDependencies,
    libraryDependencies += scalaTest
  )

lazy val shared_server = (project in file("shared/server"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslServer,
      lagomScaladslTestKit,
      scalaTest,
      filters,
      guava,
      macwire
    )
  )
  .dependsOn(authentication_service_api, hyperledger_service_api, shared_client)

lazy val hyperledger_service_api = (project in file("hyperledger_service/api"))
  .settings(
    libraryDependencies ++= apiDefaultDependencies
  )
  .dependsOn(shared_client)

lazy val hyperledger_service = (project in file("hyperledger_service/impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= implDefaultDependencies,
    libraryDependencies += lagomScaladslCluster
  )
  .settings(dockerSettings)
  .dependsOn(hyperledger_api, hyperledger_service_api, shared_server)


lazy val course_service_api = (project in file("course_service/api"))
  .settings(
    libraryDependencies ++= apiDefaultDependencies
  )
  .dependsOn(shared_client)

lazy val course_service = (project in file("course_service/impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= implDefaultDependencies,
    libraryDependencies ++= defaultPersistenceKafkaDependencies,
    libraryDependencies += uuid
  )
  .settings(dockerSettings)
  .dependsOn(course_service_api, shared_server)

lazy val hl_course_service_api = (project in file("hl_course_service/api"))
  .settings(
    libraryDependencies ++= apiDefaultDependencies
  )
  .dependsOn(course_service_api)

lazy val hl_course_service = (project in file("hl_course_service/impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= implDefaultDependencies,
    libraryDependencies += uuid
  )
  .settings(dockerSettings)
  .dependsOn(hl_course_service_api, shared_server)

lazy val authentication_service_api = (project in file("authentication_service/api"))
  .settings(
    libraryDependencies ++= apiDefaultDependencies
  )
  .dependsOn(shared_client)

lazy val authentication_service = (project in file("authentication_service/impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= implDefaultDependencies,
    libraryDependencies ++= defaultPersistenceKafkaDependencies,
  )
  .settings(dockerSettings)
  .dependsOn(authentication_service_api, user_service_api, shared_server)

lazy val user_service_api = (project in file("user_service/api"))
  .settings(
    libraryDependencies ++= apiDefaultDependencies
  )
  .dependsOn(authentication_service_api, shared_client)

lazy val user_service = (project in file("user_service/impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= implDefaultDependencies,
    libraryDependencies ++= defaultPersistenceKafkaDependencies,
  )
  .settings(dockerSettings)
  .dependsOn(user_service_api, shared_server, shared_client)

lazy val hyperledger_api = ProjectRef(file("../hyperledger/api"), "api")
