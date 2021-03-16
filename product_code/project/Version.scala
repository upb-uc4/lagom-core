
object Version {

  private val versions: Map[String, String] = Map(
    "admission_service" -> "v1.0.0",
    "authentication_service" -> "v1.0.0",
    "certificate_service" -> "v1.0.0",
    "configuration_service" -> "v1.0.0",
    "course_service" -> "v1.0.0",
    "exam_service" -> "v1.0.0",
    "examreg_service" -> "v1.0.0",
    "examresult_service" -> "v1.0.0",
    "group_service" -> "v1.0.0",
    "hyperledger_api" -> "1.0.0",
    "matriculation_service" -> "v1.0.0",
    "operation_service" -> "v1.0.0",
    "user_service" -> "v1.0.0",
    "report_service" -> "v1.0.0"
  )

  /** Returns the version of a project
    *
    * @param project name of the project
    */
  def apply(project: String): String = versions(project)
}
