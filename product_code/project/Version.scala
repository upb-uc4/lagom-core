
object Version {

  private val versions: Map[String, String] = Map(
    "authentication_service" -> "v0.11.1",
    "certificate_service" -> "v0.11.2",
    "configuration_service" -> "v0.11.1",
    "course_service" -> "v0.11.1",
    "hyperledger_api" -> "0.11.5",
    "matriculation_service" -> "v0.11.3",
    "user_service" -> "v0.11.1"
  )

  /** Returns the version of a project
    *
    * @param project name of the project
    */
  def apply(project: String): String = versions(project)
}
