import sbt.Keys._

object Commons {
  val commonSettings = Seq(
    organization := "de.upb.cs.uc4",
    version := "v0.4.4",
    scalaVersion := "2.13.0"
  )
}
