import sbt.{ Command, Project, State }

object Commands {

  val dependenciesCheck: Command = Command.single("dependenciesCheck") { (state: State, project) =>
    import state._

    println("sbt version: " + configuration.provider.id.version)
    println("Scala version (for sbt): " + configuration.provider.scalaProvider.version)
    println()

    val extracted = Project.extract(state)
    import extracted._
    println(state)
    println("Current build: " + currentRef.build)
    println("Current project: " + currentRef.project)
    println("Current subs: " + currentRef.project)
    println("Original setting count: " + session.original.size)
    println("Session setting count: " + session.append.size)

    state
  }

  val all = Seq(dependenciesCheck)
}
