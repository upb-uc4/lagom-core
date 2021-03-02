package de.upb.cs.uc4.examresult.model

import de.upb.cs.uc4.shared.client.exceptions.SimpleError
import play.api.libs.json.{ Format, Json }

import scala.concurrent.{ ExecutionContext, Future }

case class ExamResult(examResultEntries: Seq[ExamResultEntry]) {
  def trim: ExamResult = {
    copy(examResultEntries.map(_.trim))
  }

  /** Validates the object by checking predefined conditions like correct charsets, syntax, etc.
    * Returns a list of SimpleErrors[[SimpleError]]
    *
    * @return Filled Sequence of [[SimpleError]]
    */
  def validate(implicit ec: ExecutionContext): Future[Seq[SimpleError]] = {

    val errors = Future.sequence(
      examResultEntries.map { examResultEntry =>
        examResultEntry.validate.map {
          _.map {
            singleError =>
              SimpleError(s"examResultEntries[${examResultEntries.indexOf(examResultEntry)}].${singleError.name}", singleError.reason)
          }
        }
      }
    ).map(_.flatten)
    errors
  }
}

object ExamResult {
  implicit val format: Format[ExamResult] = Json.format
}