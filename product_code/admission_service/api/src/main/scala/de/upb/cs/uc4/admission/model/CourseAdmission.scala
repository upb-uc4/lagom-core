package de.upb.cs.uc4.admission.model

import play.api.libs.json.{ Format, Json }

case class CourseAdmission(
                            enrollmentId: String,
                            courseId: String,
                            moduleId: String,
                            admissionId: String,
                            timestamp: String) {

  def trim: CourseAdmission = copy(enrollmentId.trim, courseId.trim, moduleId.trim, admissionId.trim, timestamp.trim)
}

object CourseAdmission {
  implicit val format: Format[CourseAdmission] = Json.format
}
