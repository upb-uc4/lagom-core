
object Version {

  private val versions: Map[String, String] = Map(
    "authentication_service" -> "v0.9.0",
    "certificate_service" -> "v0.9.1",
    "course_service" -> "v0.8.2",
    "hyperledger_api" -> "v0.9.0",
    "matriculation_service" -> "v0.8.4",
    "user_service" -> "v0.9.2"
  )

  /** Returns the version of a project
    *
    * @param project name of the project
    */
  def apply(project: String): String = versions(project)
}
