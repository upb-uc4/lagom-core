import scala.concurrent.duration._

organization in ThisBuild := "de.upb.cs.uc4"
version in ThisBuild := "ALPHA"
lagomCassandraMaxBootWaitingTime in ThisBuild := 60.seconds

// the Scala version that will be used for cross-compiled libraries
scalaVersion in ThisBuild := "2.13.0"

val macwire = "com.softwaremill.macwire" %% "macros" % "2.3.3" % "provided"
val scalaTest = "org.scalatest" %% "scalatest" % "3.1.1" % Test

val apiDefaultDependencies = Seq(
  lagomScaladslApi
)

val implDefaultDependencies = Seq(
  lagomScaladslTestKit,
  macwire,
  scalaTest
)

val defaultCassandraKafkaDependencies = Seq(
  lagomScaladslPersistenceCassandra,
  lagomScaladslKafkaBroker
)



lazy val `lagom` = (project in file("."))
  .aggregate(`shared`, `hyperledger-service-api`, `hyperledger-service-impl`)

lazy val `shared` = (project in file("shared"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslServer,
      lagomScaladslTestKit,
      scalaTest
    )
  )

lazy val `hyperledger-service-api` = (project in file("hyperledger_service/api"))
  .settings(
    libraryDependencies ++= apiDefaultDependencies
  )

lazy val `hyperledger-service-impl` = (project in file("hyperledger_service/impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= implDefaultDependencies
  )
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
  .dependsOn(`course-service-api`, `shared`, `hyperledger-service-api`)