lazy val hyperledger_service_test = (project in file("."))
  .settings(
    name := "hyperledger_service_test",
    organization := "de.upb.cs.uc4",
    version := "v0.5.1",
    scalaVersion := "2.13.0",
    libraryDependencies ++= Seq(
        "org.scalatest" %% "scalatest" % "3.2.0" % Test,
        lagomScaladslTestKit,
        lagomScaladslApi,
        //lagomScaladslAkkaDiscovery,
    )
  )
  .dependsOn(lagom) // , hyperledger_api)
  //.enablePlugins(LagomScala)

// Tested Projects
lazy val lagom = ProjectRef(file("../../../product_code/lagom"), "lagom")

// val hyperledger_api_version = "v0.5"
// val hyperledger_api = RootProject(uri("https://github.com/upb-uc4/hyperledger_api.git#%s".format(hyperledger_api_version)))