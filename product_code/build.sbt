
// Global Settings
organization in ThisBuild := "de.upb.cs.uc4"
lagomServiceEnableSsl in ThisBuild := true
lagomCassandraEnabled in ThisBuild := false
scalaVersion in ThisBuild := "2.13.0"
scalacOptions in ThisBuild ++= Seq("-deprecation", "-feature")
lagomUnmanagedServices in ThisBuild := Map("imageprocessing" -> sys.env.getOrElse("IMAGE_PROCESSING", "http://localhost:9020"))

val withTests = "compile->compile;test->test"

// Projects
lazy val lagom = (project in file("."))
  .settings(commands ++= Commands.all)
  .aggregate(shared_client, shared_server, hyperledger_component,
    course_service_api, course_service,
    certificate_service_api, certificate_service,
    configuration_service_api, configuration_service,
    authentication_service_api, authentication_service,
    user_service_api, user_service,
    matriculation_service_api, matriculation_service,
    examreg_service_api, examreg_service,
    report_service_api, report_service)
  .dependsOn(shared_client, shared_server, hyperledger_component,
    course_service_api, course_service,
    certificate_service_api, certificate_service,
    configuration_service_api, configuration_service,
    authentication_service_api, authentication_service,
    user_service_api, user_service,
    matriculation_service_api, matriculation_service,
    examreg_service_api, examreg_service,
    report_service_api, report_service)

// This project is not allowed to have lagom server dependencies
lazy val shared_client = (project in file("shared/client"))
  .settings(
    libraryDependencies ++= Dependencies.apiDefaultDependencies,
    libraryDependencies ++= Seq(
      Dependencies.scalaTest,
      Dependencies.flexmark,
      Dependencies.janino
    )
  )
  .settings(Settings.commonSettings("shared_client"))

lazy val shared_server = (project in file("shared/server"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslServer,
      lagomScaladslTestKit,
      filters,
      Dependencies.scalaTest,
      Dependencies.flexmark,
      Dependencies.guava,
      Dependencies.macwire
    )
  )
  .settings(Settings.commonSettings("shared_server"))
  .dependsOn(shared_client, authentication_service_api)

lazy val hyperledger_component = (project in file("hyperledger_component"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslServer,
      lagomScaladslCluster,
      lagomScaladslTestKit,
      Dependencies.scalaTest,
      Dependencies.flexmark,
      Dependencies.macwire,
      Dependencies.hyperledgerApi
    ),
    version := Version("hyperledger_api")
  )
  .settings(Settings.commonSettings("hyperledger_component"))
  .dependsOn(shared_server)

lazy val course_service_api = (project in file("course_service/api"))
  .settings(Settings.apiSettings("course_service_api"))
  .settings(libraryDependencies += Dependencies.uuid % Test)
  .dependsOn(shared_client, examreg_service_api % withTests)

lazy val course_service = (project in file("course_service/impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Dependencies.defaultPersistenceKafkaDependencies,
    libraryDependencies += Dependencies.uuid
  )
  .settings(Settings.implSettings("course_service"))
  .dependsOn(course_service_api % withTests, user_service_api % withTests, examreg_service_api % withTests, shared_server % withTests)

lazy val authentication_service_api = (project in file("authentication_service/api"))
  .settings(Settings.apiSettings("authentication_service_api"))
  .dependsOn(shared_client)

lazy val authentication_service = (project in file("authentication_service/impl"))
  .enablePlugins(LagomScala)
  .settings(libraryDependencies ++= Dependencies.defaultPersistenceKafkaDependencies)
  .settings(Settings.implSettings("authentication_service"))
  .dependsOn(authentication_service_api, user_service_api % withTests, shared_server % withTests)

lazy val user_service_api = (project in file("user_service/api"))
  .settings(Settings.apiSettings("user_service_api"))
  .dependsOn(authentication_service_api % withTests, shared_client)

lazy val user_service = (project in file("user_service/impl"))
  .enablePlugins(LagomScala)
  .settings(libraryDependencies ++= Dependencies.defaultPersistenceKafkaDependencies)
  .settings(Settings.implSettings("user_service"))
  .dependsOn(user_service_api % withTests, image_processing_api % withTests, shared_server % withTests, shared_client)

lazy val matriculation_service_api = (project in file("matriculation_service/api"))
  .settings(Settings.apiSettings("matriculation_service_api"))
  .dependsOn(examreg_service_api % withTests, shared_client)

lazy val matriculation_service = (project in file("matriculation_service/impl"))
  .enablePlugins(LagomScala)
  .settings(libraryDependencies += lagomScaladslKafkaBroker)
  .settings(Settings.implSettings("matriculation_service"))
  .dependsOn(user_service_api % withTests, certificate_service_api % withTests, shared_server % withTests, shared_client, matriculation_service_api, hyperledger_component, examreg_service_api % withTests)

lazy val certificate_service_api = (project in file("certificate_service/api"))
  .settings(Settings.apiSettings("certificate_service_api"))
  .dependsOn(shared_client)

lazy val certificate_service = (project in file("certificate_service/impl"))
  .enablePlugins(LagomScala)
  .settings(libraryDependencies ++= Dependencies.defaultPersistenceKafkaDependencies)
  .settings(Settings.implSettings("certificate_service"))
  .dependsOn(certificate_service_api % withTests, user_service_api % withTests, shared_server % withTests, hyperledger_component)

lazy val configuration_service_api =  (project in file("configuration_service/api"))
  .settings(Settings.apiSettings("configuration_service_api"))
  .dependsOn(shared_client)

lazy val configuration_service = (project in file("configuration_service/impl"))
  .enablePlugins(LagomScala)
  .settings(libraryDependencies ++= Dependencies.implDefaultDependencies)
  .settings(Settings.implSettings("configuration_service"))
  .dependsOn(configuration_service_api % withTests, shared_client, shared_server % withTests)

lazy val image_processing_api = (project in file("image_processing/api"))
  .settings(libraryDependencies ++= Dependencies.apiDefaultDependencies)
  .dependsOn(shared_client)

lazy val examreg_service_api =  (project in file("examreg_service/api"))
  .settings(Settings.apiSettings("examreg_service_api"))
  .dependsOn(shared_client)

lazy val examreg_service = (project in file("examreg_service/impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Dependencies.implDefaultDependencies,
    libraryDependencies ++= Dependencies.defaultPersistenceKafkaDependencies
  )
  .settings(Settings.implSettings("examreg_service"))
  .dependsOn(examreg_service_api % withTests, shared_client % withTests, shared_server % withTests, hyperledger_component)

lazy val report_service_api =  (project in file("report_service/api"))
  .settings(Settings.apiSettings("report_service_api"))
  .dependsOn(shared_client)

lazy val report_service = (project in file("report_service/impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Dependencies.implDefaultDependencies,
    libraryDependencies ++= Dependencies.defaultPersistenceKafkaDependencies
  )
  .settings(Settings.implSettings("report_service"))
  .dependsOn(report_service_api % withTests,
    user_service_api % withTests, certificate_service_api % withTests, matriculation_service_api % withTests, course_service_api % withTests,
    shared_client % withTests, shared_server % withTests)
