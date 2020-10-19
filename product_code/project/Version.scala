
object Version {

  private val versions: Map[String, String] = Map(
    "authentication_service" -> "v0.10.0",
    "certificate_service" -> "v0.10.3",
    "configuration_service" -> "v0.10.1",
    "course_service" -> "v0.10.0",
    "hyperledger_api" -> "v0.9.2",
    "matriculation_service" -> "v0.10.1",
    "user_service" -> "v0.10.1"
  )

  /** Returns the version of a project
    *
    * @param project name of the project
    */
  def apply(project: String): String = versions(project)
}
