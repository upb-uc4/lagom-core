package de.upb.cs.uc4.exam.model

import de.upb.cs.uc4.shared.client.configuration.{ConfigurationCollection, ErrorMessageCollection, ExamType, RegexCollection}
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
    val nonEmpty100Regex = RegexCollection.Commons.nonEmpty100CharRegex
    val nonEmpty100Message = ErrorMessageCollection.Commons.nonEmpty100CharRegex
    val nonEmptyRegex = RegexCollection.Commons.nonEmptyCharRegex
    val nonEmptyMessage = ErrorMessageCollection.Commons.nonEmptyCharRegex

    var errors = List[SimpleError]()

    //TODO inquiry if examId must be empty for hyperledger to set it, or if it should be set by the frontend
    //if (!nonEmptyRegex.matches(examId)){
    //  errors :+= SimpleError("examId", nonEmptyMessage)
    //}
    if (!nonEmptyRegex.matches(courseId)){
      errors :+= SimpleError("courseId", nonEmptyMessage)
    }
    if (!nonEmptyRegex.matches(moduleId)){
      errors :+= SimpleError("moduleId", nonEmptyMessage)
    }
    if (!nonEmptyRegex.matches(lecturerEnrollmentId)){
      errors :+= SimpleError("lecturerEnrollmentId", nonEmptyMessage)
    }
    if (!ExamType.All.map(_.toString).contains(`type`)){
      errors :+= SimpleError("type", "Type must be one of " + ExamType.All.map(_.toString).reduce((a, b) => a + ", " + b) + ".")
    }
    if (!nonEmpty100Regex.matches(date)){
      errors :+= SimpleError("date", nonEmpty100Message)
    }
    if (!nonEmpty100Regex.matches(admittableUntil)){
      errors :+= SimpleError("admittableUntil", nonEmpty100Message)
    }
    if (!nonEmpty100Regex.matches(droppableUntil)){
      errors :+= SimpleError("droppableUntil", nonEmpty100Message)
    }

    errors
  }
}

object Exam {
  implicit val format: Format[Exam] = Json.format
}
