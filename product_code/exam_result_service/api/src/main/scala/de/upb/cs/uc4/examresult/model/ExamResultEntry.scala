package de.upb.cs.uc4.examresult.model

import de.upb.cs.uc4.shared.client.configuration.{ ConfigurationCollection, ErrorMessageCollection, RegexCollection }
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
    val nonEmptyRegex = RegexCollection.Commons.nonEmptyCharRegex
    val nonEmptyMessage = ErrorMessageCollection.Commons.nonEmptyCharRegex

    var errors = List[SimpleError]()

    if (!nonEmptyRegex.matches(enrollmentId)) {
      errors :+= SimpleError("enrollmentId", nonEmptyMessage)
    }
    if (!nonEmptyRegex.matches(examId)) {
      errors :+= SimpleError("examId", nonEmptyMessage)
    }
    if (!ConfigurationCollection.grades.contains(grade)) {
      errors :+= SimpleError("grade", "Grade must be one of " + ConfigurationCollection.grades.reduce((a, b) => a + ", " + b) + ".")
    }
    errors
  }
}

object ExamResultEntry {
  implicit val format: Format[ExamResultEntry] = Json.format
}