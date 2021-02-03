package de.upb.cs.uc4.exam.model

import de.upb.cs.uc4.shared.client.exceptions.SimpleError
import play.api.libs.json.{Format, Json}
import scala.concurrent.{ExecutionContext, Future}

case class Exam(examId: String, courseId: String, moduleId: String, lecturerEnrollmentId: String, `type`: String, date: String, ects: Int, admittableUntil: String, droppableUntil: String) {
  def trim: Exam = {
    copy(examId.trim, courseId.trim, moduleId.trim, lecturerEnrollmentId.trim, `type`.trim, date.trim, ects, admittableUntil.trim, droppableUntil.trim)
  }

  /** Validates the object by checking predefined conditions like correct charsets, syntax, etc.
    * Returns a list of SimpleErrors[[SimpleError]]
    *
    * @return Filled Sequence of [[SimpleError]]
    */
  def validate(implicit ec: ExecutionContext): Future[Seq[SimpleError]] = Future {
    // TODO validation
    var errors = List[SimpleError]()

    errors
  }
}

object Exam {
  implicit val format: Format[Exam] = Json.format
}
