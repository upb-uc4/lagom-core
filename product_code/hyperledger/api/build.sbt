lazy val hyperledger_api = (project in file("."))
  .settings(
    name := "hyperledger_api",
    Commons.commonSettings,
    libraryDependencies ++= Dependencies.scalaTestDependencies,
    libraryDependencies ++= Dependencies.hyperledgerDependencies,
  )