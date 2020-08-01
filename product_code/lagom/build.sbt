import com.typesafe.sbt.packager.docker.DockerChmodType

// apply common settings
Commons.commonSettings

lagomServiceEnableSsl in ThisBuild := true
// The project uses PostgreSQL
lagomCassandraEnabled in ThisBuild := false

// Docker
def dockerSettings = Seq(
  dockerUpdateLatest := true,
  dockerBaseImage := "adoptopenjdk/openjdk8",
  dockerUsername := Some("uc4official"),
  dockerChmodType := DockerChmodType.UserGroupWriteExecute,
)


// Projects
lazy val lagom = (project in file("."))
  .aggregate(shared_client, shared_server,
    course_service_api, course_service,
    hl_course_service_api, hl_course_service,
    hyperledger_service_api, hyperledger_service,
    authentication_service_api, authentication_service,
    user_service_api, user_service,
    matriculation_service_api, matriculation_service)
  .dependsOn(shared_client, shared_server,
    course_service_api, course_service,
    hl_course_service_api, hl_course_service,
    hyperledger_service_api, hyperledger_service,
    authentication_service_api, authentication_service,
    user_service_api, user_service,
    matriculation_service_api, matriculation_service)

// This project is not allowed to have lagom server dependencies
lazy val shared_client = (project in file("shared/client"))
  .settings(
    libraryDependencies ++= Dependencies.apiDefaultDependencies,
    libraryDependencies ++= Dependencies.scalaTestDependencies
  )

lazy val shared_server = (project in file("shared/server"))
  .settings(
    libraryDependencies ++=Dependencies.sharedServerDependencies
  )
  .dependsOn(authentication_service_api, hyperledger_service_api, shared_client)

lazy val hyperledger_service_api = (project in file("hyperledger_service/api"))
  .settings(
    libraryDependencies ++= Dependencies.apiDefaultDependencies
  )
  .dependsOn(shared_client)

lazy val hyperledger_service = (project in file("hyperledger_service/impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Dependencies.implDefaultDependencies,
    libraryDependencies += lagomScaladslCluster
  )
  .settings(dockerSettings)
  .dependsOn(hyperledger_api, hyperledger_service_api, shared_server)


lazy val course_service_api = (project in file("course_service/api"))
  .settings(
    libraryDependencies ++= Dependencies.apiDefaultDependencies
  )
  .dependsOn(shared_client)

lazy val course_service = (project in file("course_service/impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Dependencies.implDefaultDependencies,
    libraryDependencies ++= Dependencies.defaultPersistenceKafkaDependencies,
    libraryDependencies ++= Dependencies.uuidDependencies
  )
  .settings(dockerSettings)
  .dependsOn(course_service_api, shared_server)

lazy val hl_course_service_api = (project in file("hl_course_service/api"))
  .settings(
    libraryDependencies ++= Dependencies.apiDefaultDependencies
  )
  .dependsOn(course_service_api)

lazy val hl_course_service = (project in file("hl_course_service/impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Dependencies.implDefaultDependencies,
    libraryDependencies ++= Dependencies.uuidDependencies
  )
  .settings(dockerSettings)
  .dependsOn(hl_course_service_api, shared_server)

lazy val authentication_service_api = (project in file("authentication_service/api"))
  .settings(
    libraryDependencies ++= Dependencies.apiDefaultDependencies
  )
  .dependsOn(shared_client)

lazy val authentication_service = (project in file("authentication_service/impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Dependencies.implDefaultDependencies,
    libraryDependencies ++= Dependencies.defaultPersistenceKafkaDependencies
  )
  .settings(dockerSettings)
  .dependsOn(authentication_service_api, user_service_api, shared_server)

lazy val user_service_api = (project in file("user_service/api"))
  .settings(
    libraryDependencies ++= Dependencies.apiDefaultDependencies
  )
  .dependsOn(authentication_service_api, matriculation_service_api, shared_client)

lazy val user_service = (project in file("user_service/impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Dependencies.implDefaultDependencies,
    libraryDependencies ++= Dependencies.defaultPersistenceKafkaDependencies
  )
  .settings(dockerSettings)
  .dependsOn(user_service_api, shared_server)

lazy val matriculation_service_api = (project in file("matriculation_service/api"))
  .settings(
    libraryDependencies ++= Dependencies.apiDefaultDependencies
  )
  .dependsOn(authentication_service_api, shared_client)

lazy val matriculation_service = (project in file("matriculation_service/impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Dependencies.implDefaultDependencies
  )
  .settings(dockerSettings)
  .dependsOn(matriculation_service_api, shared_server)

lazy val hyperledger_api = ProjectRef(file("../hyperledger/api"), "hyperledger_api")
