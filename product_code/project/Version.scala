
object Version {

  private val versions: Map[String, String] = Map(
    "authentication_service" -> "v0.13.1",
    "certificate_service" -> "v0.13.1",
    "configuration_service" -> "v0.13.1",
    "course_service" -> "v0.13.1",
    "examreg_service" -> "v0.13.1",
    "hyperledger_api" -> "0.14.4",
    "matriculation_service" -> "v0.13.2",
    "user_service" -> "v0.13.3"
  )

  /** Returns the version of a project
    *
    * @param project name of the project
    */
  def apply(project: String): String = versions(project)
}
