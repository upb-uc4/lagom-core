
object Version {

  private val versions: Map[String, String] = Map(
    "authentication_service" -> "v0.12.0",
    "certificate_service" -> "v0.12.0",
    "configuration_service" -> "v0.12.0",
    "course_service" -> "v0.12.0",
    "examreg_service" -> "0.12.1",
    "hyperledger_api" -> "0.11.5",
    "matriculation_service" -> "v0.12.0",
    "user_service" -> "v0.12.0"
  )

  /** Returns the version of a project
    *
    * @param project name of the project
    */
  def apply(project: String): String = versions(project)
}
