package de.upb.cs.uc4.admission.model

import de.upb.cs.uc4.shared.client.configuration.{ ErrorMessageCollection, RegexCollection }
import de.upb.cs.uc4.shared.client.exceptions.SimpleError
import play.api.libs.json.{ Format, Json }

import scala.concurrent.{ ExecutionContext, Future }

case class CourseAdmission(
    enrollmentId: String,
    courseId: String,
    moduleId: String,
    admissionId: String,
    timestamp: String
) {

  def trim: CourseAdmission = copy(enrollmentId.trim, courseId.trim, moduleId.trim, admissionId.trim, timestamp.trim)

  def validateOnCreation(implicit ec: ExecutionContext): Future[Seq[SimpleError]] = Future {
    var errors = List[SimpleError]()
    val nonEmptyRegex = RegexCollection.Commons.nonEmptyCharRegex
    val nonEmptyMessage = ErrorMessageCollection.Commons.nonEmptyCharRegex

    if (!nonEmptyRegex.matches(enrollmentId)) {
      errors :+= SimpleError("enrollmentId", nonEmptyMessage)
    }
    if (!nonEmptyRegex.matches(courseId)) {
      errors :+= SimpleError("courseId", nonEmptyMessage)
    }
    if (!nonEmptyRegex.matches(moduleId)) {
      errors :+= SimpleError("moduleId", nonEmptyMessage)
    }
    if (admissionId.nonEmpty) {
      errors :+= SimpleError("admissionId", "AdmissionId must be empty.")
    }
    if (timestamp.nonEmpty) {
      errors :+= SimpleError("timestamp", "Timestamp must be empty.")
    }

    errors
  }
}

object CourseAdmission {
  implicit val format: Format[CourseAdmission] = Json.format
}
