
object Version {

  private val versions: Map[String, String] = Map(
    "admission_service" -> "v0.15.1",
    "authentication_service" -> "v0.14.1",
    "certificate_service" -> "v0.14.1",
    "configuration_service" -> "v0.15.0",
    "course_service" -> "v0.14.1",
    "examreg_service" -> "v0.15.0",
    "group_service" -> "v0.15.1",
    "hyperledger_api" -> "0.15.2",
    "matriculation_service" -> "v0.14.2",
    "operation_service" -> "v0.15.1",
    "user_service" -> "v0.14.1"
  )

  /** Returns the version of a project
    *
    * @param project name of the project
    */
  def apply(project: String): String = versions(project)
}
