
object Version {

  private val versions: Map[String, String] = Map(
    "admission_service" -> "v0.15.1",
    "authentication_service" -> "v0.14.1",
    "certificate_service" -> "v0.14.1",
    "configuration_service" -> "v0.15.1",
    "course_service" -> "v0.14.1",
    "examreg_service" -> "v0.16.0",
    "hyperledger_api" -> "0.14.5",
    "matriculation_service" -> "v0.15.1",
    "user_service" -> "v0.15.2",
    "report_service" -> "v0.16.0"
  )

  /** Returns the version of a project
    *
    * @param project name of the project
    */
  def apply(project: String): String = versions(project)
}
