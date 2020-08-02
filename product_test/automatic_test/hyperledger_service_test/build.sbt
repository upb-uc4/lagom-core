lazy val hyperledger_service_test = (project in file("."))
  .settings(
    name := "hyperledger_service_test",
    organization := "de.upb.cs.uc4",
    version := "v0.3.0",
    scalaVersion := "2.13.0",
    libraryDependencies ++= Seq(
        "org.scalatest" %% "scalatest" % "3.2.0" % Test,
        lagomScaladslTestKit,
        lagomScaladslApi,
        //lagomScaladslAkkaDiscovery,
    )
  )
  .dependsOn(lagom)//, api)
  //.enablePlugins(LagomScala)

// Tested Projects
lazy val lagom = ProjectRef(file("../../../product_code/lagom"), "lagom")
// lazy val api = ProjectRef(file("../../../product_code/hyperledger/api"), "api")