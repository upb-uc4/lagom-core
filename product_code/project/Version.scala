
object Version {

  private val versions: Map[String, String] = Map(
    "authentication_service" -> "v0.11.0",
    "certificate_service" -> "v0.11.0",
    "configuration_service" -> "v0.11.0",
    "course_service" -> "v0.11.0",
    "hyperledger_api" -> "0.11.3",
    "matriculation_service" -> "v0.11.1",
    "user_service" -> "v0.11.0"
  )

  /** Returns the version of a project
    *
    * @param project name of the project
    */
  def apply(project: String): String = versions(project)
}
