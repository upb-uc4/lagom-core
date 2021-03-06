// The Lagom plugin
addSbtPlugin("com.lightbend.lagom" % "lagom-sbt-plugin" % "1.6.4")
// Not needed once upgraded to Play 2.7.1, we are on 1.6.* Play or so idk
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.19")
// Enables code coverage
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.6.0")
// Scala auto formatting tool
addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.8.3")
