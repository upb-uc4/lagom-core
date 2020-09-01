Commons.commonSettings

lazy val hyperledger_service_test = (project in file("."))
  .settings(
    name := "hyperledger_service_test",
    libraryDependencies ++= Dependencies.scalaTestDependencies,
    libraryDependencies ++= Dependencies.lagomDependencies
  )
  .dependsOn(lagom)

// Tested Projects
lazy val lagom = ProjectRef(file("../../../product_code/lagom"), "lagom")