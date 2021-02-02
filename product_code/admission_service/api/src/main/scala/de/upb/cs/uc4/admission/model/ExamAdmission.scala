package de.upb.cs.uc4.admission.model

import de.upb.cs.uc4.shared.client.configuration.{ ErrorMessageCollection, RegexCollection }
import de.upb.cs.uc4.shared.client.exceptions.SimpleError
import play.api.libs.json.{ Format, Json }

import scala.concurrent.{ ExecutionContext, Future }

case class ExamAdmission(
  admissionId:	String,
  enrollmentId: String,
  timestamp:	String,
  `type`:	String,
  examId: String) extends AbstractAdmission {

  override def copyAdmission(admissionId: String, enrollmentId: String, timestamp: String, `type`: String): ExamAdmission = copy(admissionId, enrollmentId, timestamp, `type`)

  override def trim: ExamAdmission =
    super.trim.asInstanceOf[ExamAdmission].copy(examId = examId.trim)

  override def validateOnCreation(implicit ec: ExecutionContext): Future[Seq[SimpleError]] = {
    val errorsFuture = super.validateOnCreation
    errorsFuture.map { errors: Seq[SimpleError] =>

      val nonEmptyRegex = RegexCollection.Commons.nonEmptyCharRegex
      val nonEmptyMessage = ErrorMessageCollection.Commons.nonEmptyCharRegex
      var extendedErrors = errors

      if (!nonEmptyRegex.matches(examId)) {
        extendedErrors :+= SimpleError("examId", nonEmptyMessage)
      }

      extendedErrors
    }
  }
}

object ExamAdmission {
  implicit val format: Format[ExamAdmission] = Json.format
}