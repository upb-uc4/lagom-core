lazy val hyperledger_api = (project in file("."))
  .settings(
    Commons.commonSettings,
    name := "hyperledger_api",
    libraryDependencies ++= Dependencies.scalaTestDependencies,
    libraryDependencies ++= Dependencies.hyperledgerDependencies,
  )