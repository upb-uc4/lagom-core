import sbt._
import com.lightbend.lagom.sbt.LagomImport.{lagomScaladslApi, lagomScaladslTestKit}

object Dependencies {
  // libraries
  private val scala_test = "org.scalatest" %% "scalatest" % "3.2.0" % Test

  // dependency groups
  val scalaTestDependencies = Seq(scala_test)
  val lagomTestDependencies = Seq(lagomScaladslTestKit, lagomScaladslApi)
}