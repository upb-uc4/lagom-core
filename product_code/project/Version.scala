
object Version {

  private val versions: Map[String, String] = Map(
    "admission_service" -> "v0.19.2",
    "authentication_service" -> "v0.18.0",
    "certificate_service" -> "v0.19.1",
    "configuration_service" -> "v0.19.1",
    "course_service" -> "v0.17.1",
    "exam_service" -> "v0.19.2",
    "examreg_service" -> "v0.19.1",
    "examresult_service" -> "v0.19.2",
    "group_service" -> "v0.19.1",
    "hyperledger_api" -> "0.18.0",
    "matriculation_service" -> "v0.19.1",
    "operation_service" -> "v0.19.2",
    "user_service" -> "v0.17.1",
    "report_service" -> "v0.19.2"
  )

  /** Returns the version of a project
    *
    * @param project name of the project
    */
  def apply(project: String): String = versions(project)
}
