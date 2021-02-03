package de.upb.cs.uc4.examresult.model

import de.upb.cs.uc4.shared.client.exceptions.SimpleError
import play.api.libs.json.{ Format, Json }

import scala.concurrent.{ ExecutionContext, Future }

case class ExamResultEntry(enrollmentId: String, examId: String, grade: String) {
  def trim: ExamResultEntry = {
    copy(enrollmentId.trim, examId.trim, grade.trim)
  }

  /** Validates the object by checking predefined conditions like correct charsets, syntax, etc.
    * Returns a list of SimpleErrors[[SimpleError]]
    *
    * @return Filled Sequence of [[SimpleError]]
    */
  def validate(implicit ec: ExecutionContext): Future[Seq[SimpleError]] = Future {
    var errors = List[SimpleError]()

    if (enrollmentId.isEmpty) {
      errors :+= SimpleError("enrollmentId", "EnrollmentId must not be empty.")
    }
    if (examId.isEmpty) {
      errors :+= SimpleError("examId", "ExamId must not be empty.")
    }
    if (grade.isEmpty) {
      errors :+= SimpleError("grade", "Grade must not be empty.")
    }
    errors
  }
}

object ExamResultEntry {
  implicit val format: Format[ExamResultEntry] = Json.format
}