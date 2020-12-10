
object Version {

  private val versions: Map[String, String] = Map(
    "authentication_service" -> "v0.14.1",
    "certificate_service" -> "v0.14.1",
    "configuration_service" -> "v0.14.1",
    "course_service" -> "v0.14.1",
    "examreg_service" -> "v0.14.1",
    "hyperledger_api" -> "014.1",
    "matriculation_service" -> "v0.14.1",
    "user_service" -> "v0.14.1"
  )

  /** Returns the version of a project
    *
    * @param project name of the project
    */
  def apply(project: String): String = versions(project)
}
