import com.typesafe.sbt.packager.docker.DockerChmodType
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport.{ dockerBaseImage, dockerChmodType, dockerUpdateLatest, dockerUsername }
import sbt.Keys._
import sbt.{ Def, TestFrameworks, Tests }

object Settings {

  private def dockerSettings(project: String) = Seq(
    dockerUpdateLatest := true,
    dockerBaseImage := "adoptopenjdk/openjdk8",
    dockerUsername := Some("uc4official"),
    dockerChmodType := DockerChmodType.UserGroupWriteExecute,
    version := Version(project)
  )

  /** The settings every project needs
    *
    * @param project name of the project
    */
  def commonSettings(project: String): Seq[Def.Setting[_]] = Seq(
    testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-h", "target/test_reports/" + project)
  )

  /** The settings every api project needs
    * This includes the common settings
    * and the needed dependencies
    *
    * @param project name of the project
    */
  def apiSettings(project: String): Seq[Def.Setting[_]] =
    commonSettings(project) ++ Seq(libraryDependencies ++= Dependencies.apiDefaultDependencies)

  /** The settings every impl project needs
    * This includes the common settings
    * and the needed dependencies
    *
    * @param project name of the project
    */
  def implSettings(project: String): Seq[Def.Setting[_]] =
    commonSettings(project) ++ dockerSettings(project) ++ Seq(libraryDependencies ++= Dependencies.implDefaultDependencies)
}
