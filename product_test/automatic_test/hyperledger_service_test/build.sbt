
lazy val hyperledger_service_test = (project in file("."))
  .settings(
    name := "hyperledger_service_test",
    Commons.commonSettings,
    libraryDependencies ++= Dependencies.scalaTestDependencies,
    libraryDependencies ++= Dependencies.lagomTestDependencies
  ).dependsOn(lagom)

// Tested Projects
lazy val lagom = ProjectRef(file("../../../product_code/lagom"), "lagom")