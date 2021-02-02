package de.upb.cs.uc4.admission.model

import de.upb.cs.uc4.shared.client.configuration.{ ErrorMessageCollection, RegexCollection }
import de.upb.cs.uc4.shared.client.exceptions.SimpleError
import play.api.libs.json.{ Format, Json }

import scala.concurrent.{ ExecutionContext, Future }

case class CourseAdmission(
    admissionId: String,
    enrollmentId: String,
    timestamp: String,
    `type`: String,
    courseId: String,
    moduleId: String
) extends AbstractAdmission {

  override def copyAdmission(admissionId: String, enrollmentId: String, timestamp: String, `type`: String): CourseAdmission =
    copy(admissionId, enrollmentId, timestamp, `type`)

  override def trim: CourseAdmission =
    super.trim.asInstanceOf[CourseAdmission].copy(courseId = courseId.trim, moduleId = moduleId.trim)

  override def validateOnCreation(implicit ec: ExecutionContext): Future[Seq[SimpleError]] = {
    val errorsFuture = super.validateOnCreation
    errorsFuture.map { errors: Seq[SimpleError] =>

      val nonEmptyRegex = RegexCollection.Commons.nonEmptyCharRegex
      val nonEmptyMessage = ErrorMessageCollection.Commons.nonEmptyCharRegex
      var extendedErrors = errors

      if (!nonEmptyRegex.matches(courseId)) {
        extendedErrors :+= SimpleError("courseId", nonEmptyMessage)
      }
      if (!nonEmptyRegex.matches(moduleId)) {
        extendedErrors :+= SimpleError("moduleId", nonEmptyMessage)
      }

      extendedErrors
    }
  }
}

object CourseAdmission {
  implicit val format: Format[CourseAdmission] = Json.format
}
