import com.typesafe.sbt.packager.docker.DockerChmodType

organization in ThisBuild := "de.upb.cs.uc4"
lagomServiceEnableSsl in ThisBuild := true
val hyperledgerApiVersion = "v0.5.4"

// The project uses PostgreSQL
lagomCassandraEnabled in ThisBuild := false

// the Scala version that will be used for cross-compiled libraries
scalaVersion in ThisBuild := "2.13.0"


def commonSettings(project: String) = Seq(
  testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-h", "target/test_reports/" + project)
)

// Docker
val dockerSettings = Seq(
  dockerUpdateLatest := true,
  dockerBaseImage := "adoptopenjdk/openjdk8",
  dockerUsername := Some("uc4official"),
  dockerChmodType := DockerChmodType.UserGroupWriteExecute,
)

// Dependencies
val macwire = "com.softwaremill.macwire" %% "macros" % "2.3.3" % "provided"
val scalaTest = "org.scalatest" %% "scalatest" % "3.2.0" % Test
val flexmark = "com.vladsch.flexmark" % "flexmark-all" % "0.35.10" % Test
val guava = "com.google.guava" % "guava" % "29.0-jre"
val akkaDiscoveryKubernetes = "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % "1.0.8"
val postgresDriver = "org.postgresql" % "postgresql" % "42.2.8"
val uuid = "com.fasterxml.uuid" % "java-uuid-generator" % "3.1.0"
val janino = "org.codehaus.janino" % "janino" % "2.5.16"
val hyperledger_api = RootProject(uri("https://github.com/upb-uc4/hyperledger_api.git#%s".format(hyperledgerApiVersion)))

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
  lagomScaladslKafkaBroker,
)

// Projects
lazy val lagom = (project in file("."))
  .aggregate(shared_client, shared_server,
    course_service_api, course_service,
    hl_course_service_api, hl_course_service,
    hyperledger_service_api, hyperledger_service,
    authentication_service_api, authentication_service,
    user_service_api, user_service,
    matriculation_service_api, matriculation_service)
  .dependsOn(shared_client, shared_server,
    course_service_api, course_service,
    hl_course_service_api, hl_course_service,
    hyperledger_service_api, hyperledger_service,
    authentication_service_api, authentication_service,
    user_service_api, user_service,
    matriculation_service_api, matriculation_service)

// This project is not allowed to have lagom server dependencies
lazy val shared_client = (project in file("shared/client"))
  .settings(
    libraryDependencies ++= apiDefaultDependencies,
    libraryDependencies += scalaTest,
    libraryDependencies += flexmark,
    libraryDependencies += janino
  )
  .settings(commonSettings("shared_client"))


lazy val shared_server = (project in file("shared/server"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslServer,
      lagomScaladslTestKit,
      scalaTest,
      flexmark,
      filters,
      guava,
      macwire
    )
  )
  .settings(commonSettings("shared_server"))
  .dependsOn(authentication_service_api, hyperledger_service_api, shared_client)

lazy val hyperledger_service_api = (project in file("hyperledger_service/api"))
  .settings(
    libraryDependencies ++= apiDefaultDependencies
  )
  .settings(commonSettings("hyperledger_service_api"))
  .dependsOn(shared_client)

lazy val hyperledger_service = (project in file("hyperledger_service/impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= implDefaultDependencies,
    libraryDependencies += lagomScaladslCluster,
    mappings in Docker += file("hyperledger_service/impl/src/main/resources/hyperledger_assets/connection_profile_release.yaml")
      -> "opt/docker/share/hyperledger_assets/connection_profile.yaml",
    mappings in Docker += file("hyperledger_service/impl/src/main/resources/hyperledger_assets/wallet/cli.id")
      -> "opt/docker/share/hyperledger_assets/wallet/cli.id",
  )
  .settings(commonSettings("hyperledger_service"))
  .settings(dockerSettings)
  .settings(version := "v0.6.0")
  .dependsOn(hyperledger_api, hyperledger_service_api, shared_server)

lazy val course_service_api = (project in file("course_service/api"))
  .settings(
    libraryDependencies ++= apiDefaultDependencies
  )
  .settings(commonSettings("course_service_api"))
  .dependsOn(shared_client)

lazy val course_service = (project in file("course_service/impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= implDefaultDependencies,
    libraryDependencies ++= defaultPersistenceKafkaDependencies,
    libraryDependencies += uuid
  )
  .settings(commonSettings("course_service"))
  .settings(dockerSettings)
  .settings(version := "v0.6.0")
  .dependsOn(course_service_api, shared_server)

lazy val hl_course_service_api = (project in file("hl_course_service/api"))
  .settings(
    libraryDependencies ++= apiDefaultDependencies
  )
  .settings(commonSettings("hl_course_service_api"))
  .dependsOn(course_service_api)

lazy val hl_course_service = (project in file("hl_course_service/impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= implDefaultDependencies,
    libraryDependencies += uuid
  )
  .settings(commonSettings("hl_course_service"))
  .settings(dockerSettings)
  .settings(version := "v0.6.0")
  .dependsOn(hl_course_service_api, shared_server)

lazy val authentication_service_api = (project in file("authentication_service/api"))
  .settings(
    libraryDependencies ++= apiDefaultDependencies
  )
  .settings(commonSettings("authentication_service_api"))
  .dependsOn(shared_client)

lazy val authentication_service = (project in file("authentication_service/impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= implDefaultDependencies,
    libraryDependencies ++= defaultPersistenceKafkaDependencies,
  )
  .settings(commonSettings("authentication_service"))
  .settings(dockerSettings)
  .settings(version := "v0.6.0")
  .dependsOn(authentication_service_api, user_service_api, shared_server)

lazy val user_service_api = (project in file("user_service/api"))
  .settings(
    libraryDependencies ++= apiDefaultDependencies
  )
  .settings(commonSettings("user_service_api"))
  .dependsOn(authentication_service_api, matriculation_service_api, shared_client)

lazy val user_service = (project in file("user_service/impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= implDefaultDependencies,
    libraryDependencies ++= defaultPersistenceKafkaDependencies,
  )
  .settings(commonSettings("user_service"))
  .settings(dockerSettings)
  .settings(version := "v0.6.0")
  .dependsOn(user_service_api, shared_server, shared_client)

lazy val matriculation_service_api = (project in file("matriculation_service/api"))
  .settings(
    libraryDependencies ++= apiDefaultDependencies
  )
  .settings(commonSettings("matriculation_service_api"))
  .dependsOn(authentication_service_api, shared_client)

lazy val matriculation_service = (project in file("matriculation_service/impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= implDefaultDependencies,
    libraryDependencies += lagomScaladslKafkaBroker
  )
  .settings(commonSettings("matriculation_service"))
  .settings(dockerSettings)
  .settings(version := "v0.6.0")
  .dependsOn(user_service_api, shared_server, shared_client, matriculation_service_api)
