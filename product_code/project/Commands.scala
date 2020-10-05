import java.io.{ File, PrintWriter }

import sbt.internal.util.ConsoleLogger
import sbt.{ Command, Project, State }

object Commands {

  private lazy val logger = ConsoleLogger()

  val versionCheck: Command = Command.single("versionCheck") { (state: State, tag: String) =>

    val tagRegex = """([A-Za-z]*)-(v[0-9]+\.[0-9]+\.[0-9]+)""".r

    try {
      val tagRegex(service, version) = tag

      try {
        if (compare(version, Version(s"${service}_service")) == 0) {
          logger.info("The version matches the tag.")
          state
        } else {
          logger.err(s"The version of $service is ${Version(s"${service}_service")}, but was tagged with $version.")
          state.fail
        }
      }
      catch {
        case _: NoSuchElementException =>
          logger.err(s"The service $service is not defined in the build.")
          state.fail
      }
    }
    catch {
      case _: Throwable =>
        logger.err(s"The tag $tag is malformed.")
        state.fail
    }
  }

  val dependencyCheck: Command = Command.single("dependencyCheck") { (state: State, shortName: String) =>
    val project = shortName + "_service"
    val extracted = Project.extract(state)

    val lagom = Project.getProject(extracted.currentRef, extracted.structure).get
    val version = Version(project)

    val writer = new PrintWriter(new File("target/dependencyCheck.txt"))
    var failed = false

    for (ref <- lagom.aggregate) {
      val depend = Project.getProject(ref, extracted.structure).get
      if (depend.uses.map(_.project).contains(project + "_api") && depend.id.contains("_service")) {
        val dependState = Command.process(s"project ${depend.id}", state)
        val dependVersion = Project.extract(dependState).get(sbt.Keys.version)

        if (compare(dependVersion, version) < 0) {
          logger.warn(s"${depend.id.stripSuffix("_api")} could be broken by a breaking change in $project.")
          writer.println(s"${depend.id.stripSuffix("_api")} with $dependVersion could be broken by a breaking change in $project on $version.")
          failed = true
        } else {
          logger.success(s"${depend.id.stripSuffix("_api")} is going to function without compatibility problems.")
        }
      }
    }

    writer.close()

    if (failed) state.fail else state
  }

  /** Compares two version strings.
    *
    * @param   version1 the first version to be compared.
    * @param   version2 the second version to be compared.
    * @return the value 0 if version1 is
    *         equal to version2; a value less than
    *         0 if version1 is smaller
    *         than version2 and a value greater
    *         than 0 if version1 is
    *         greater than version2.
    */
  private def compare(version1: String, version2: String): Int = {
    val Array(major1, minor1, patch1) = version1.substring(1).split("\\.").map(_.toInt)
    val Array(major2, minor2, patch2) = version2.substring(1).split("\\.").map(_.toInt)

    val majorDif = major1.compareTo(major2)
    val minorDif = minor1.compareTo(minor2)
    val patchDif = patch1.compareTo(patch2)

    if (majorDif == 0) if (minorDif == 0) patchDif else minorDif else majorDif
  }

  val all = Seq(versionCheck, dependencyCheck)
}
