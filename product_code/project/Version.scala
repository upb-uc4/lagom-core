
object Version {

  private val versions: Map[String, String] = Map(
    "authentication_service" -> "v0.13.1",
    "certificate_service" -> "v0.13.1",
    "configuration_service" -> "v0.13.1",
    "course_service" -> "v0.14.0",
    "examreg_service" -> "v0.13.1",
    "hyperledger_api" -> "0.11.5",
    "matriculation_service" -> "v0.13.2",
    "user_service" -> "v0.13.3"
  )

  /** Returns the version of a project
    *
    * @param project name of the project
    */
  def apply(project: String): String = versions(project)
}
