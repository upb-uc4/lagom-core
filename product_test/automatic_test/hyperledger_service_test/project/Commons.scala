import sbt.Keys._
import sbt._

object Commons {
  val commonSettings = Seq(
    organization := "de.upb.cs.uc4",
    version := "v0.5.1",
    scalaVersion := "2.13.0"
  )
}
