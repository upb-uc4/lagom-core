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
  .aggregate(`shared`, `hyperledger-api`, `hyperledger-impl`)

lazy val `shared` = (project in file("shared"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslServer,
      lagomScaladslTestKit,
      scalaTest
    )
  )

lazy val `hyperledger-api` = (project in file("hyperledger/api"))
  .settings(
    libraryDependencies ++= apiDefaultDependencies
  )

lazy val `hyperledger-impl` = (project in file("hyperledger/impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= implDefaultDependencies
  )
  .dependsOn(`hyperledger-api`, `shared`)