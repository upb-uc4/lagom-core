
object Version {

  private val versions: Map[String, String] = Map(
    "authentication_service" -> "v0.13.0",
    "certificate_service" -> "v0.13.0",
    "configuration_service" -> "v0.13.0",
    "course_service" -> "v0.13.1",
    "examreg_service" -> "v0.12.3",
    "hyperledger_api" -> "0.11.5",
    "matriculation_service" -> "v0.13.1",
    "user_service" -> "v0.13.1"
  )

  /** Returns the version of a project
    *
    * @param project name of the project
    */
  def apply(project: String): String = versions(project)
}
