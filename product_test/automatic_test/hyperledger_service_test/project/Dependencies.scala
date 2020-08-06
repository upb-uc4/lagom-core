import com.lightbend.lagom.sbt.LagomImport.{lagomScaladslApi, lagomScaladslTestKit}
import sbt._

object Dependencies {
  // libraries
  private val scalaTest = "org.scalatest" %% "scalatest" % "3.2.0" % Test

  // dependency groups
  val lagomDependencies = Seq(lagomScaladslTestKit, lagomScaladslApi)
  val scalaTestDependencies = Seq(scalaTest)
}