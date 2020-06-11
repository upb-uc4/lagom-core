import scala.concurrent.duration._

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
  dockerRepository := sys.props.get("docker.registry")
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
    `course-service-api`, `course-service-impl`,
    `hyperledger-service-api`, `hyperledger-service-impl`,
    `authentication-service-api`, `authentication-service-impl`)

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
  .dependsOn(`authentication-service-api`)

lazy val `hyperledger-service-api` = (project in file("hyperledger_service/api"))
  .settings(
    libraryDependencies ++= apiDefaultDependencies
  )

lazy val `hyperledger-service-impl` = (project in file("hyperledger_service/impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= implDefaultDependencies
  )
  .settings(dockerSettings)
  .dependsOn(`hyperledger-service-api`, `shared`)

lazy val `course-service-api` = (project in file("course_service/api"))
  .settings(
    libraryDependencies ++= apiDefaultDependencies
  )

lazy val `course-service-impl` = (project in file("course_service/impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= implDefaultDependencies,
    libraryDependencies ++= defaultCassandraKafkaDependencies
  )
  .settings(dockerSettings)
  .dependsOn(`course-service-api`, `shared`, `hyperledger-service-api`)

lazy val `authentication-service-api` = (project in file("authentication_service/api"))
  .settings(
    libraryDependencies ++= apiDefaultDependencies
  )
  .dependsOn(`user-service-api`)

lazy val `authentication-service-impl` = (project in file("authentication_service/impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= implDefaultDependencies,
    libraryDependencies ++= defaultCassandraKafkaDependencies
  )
  .settings(dockerSettings)
  .dependsOn(`authentication-service-api`,  `shared`)

lazy val `user-service-api` = (project in file("user_service/api"))
  .settings(
    libraryDependencies ++= apiDefaultDependencies
  )