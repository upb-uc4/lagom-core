import sbt.internal.util.ConsoleLogger
import sbt.{ Command, State }

object Commands {

  val versionCheck: Command = Command.single("versionCheck") { (state: State, tag: String) =>

    lazy val logger = ConsoleLogger()
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

  val all = Seq(versionCheck)
}
