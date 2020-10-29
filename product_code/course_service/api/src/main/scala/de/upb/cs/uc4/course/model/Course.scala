package de.upb.cs.uc4.course.model

import de.upb.cs.uc4.shared.client.configuration.{ CourseLanguage, CourseType, RegexCollection }
import de.upb.cs.uc4.shared.client.exceptions.SimpleError
import play.api.libs.json._

import scala.concurrent.{ ExecutionContext, Future }

case class Course(
    courseId: String,
    moduleIds: Seq[String],
    courseName: String,
    courseType: String,
    startDate: String,
    endDate: String,
    ects: Int,
    lecturerId: String,
    maxParticipants: Int,
    currentParticipants: Int,
    courseLanguage: String,
    courseDescription: String
) {
  def trim: Course = {
    copy(
      courseId,
      moduleIds,
      courseName.trim,
      courseType,
      startDate.trim,
      endDate.trim,
      ects,
      lecturerId.trim,
      maxParticipants,
      currentParticipants,
      courseLanguage,
      courseDescription.trim
    )
  }

  /** Checks if the course attributes correspond to agreed syntax and semantics
    *
    * @return response-code which gives detailed description of syntax or semantics violation
    */
  def validate(implicit ec: ExecutionContext): Future[Seq[SimpleError]] = Future {

    val nameRegex = RegexCollection.Commons.nameRegex
    val descriptionRegex = RegexCollection.Commons.longTextRegex
    val dateRegex = RegexCollection.Commons.dateRegex
    val ectsRegex = RegexCollection.Course.ectsRegex
    val maxParticipantsRegex = RegexCollection.Course.maxParticipantsRegex

    var errors = List[SimpleError]()

    courseName match {
      case "" => errors :+= SimpleError("courseName", "Course name must not be empty.")
      case _ if !nameRegex.matches(courseName) => errors :+= SimpleError("courseName", "Course name must not contain more than 100 characters.")
      case _ =>
    }
    if (!CourseType.All.map(_.toString).contains(courseType)) {
      errors :+= SimpleError("courseType", "Course type must be one of " + CourseType.All.map(_.toString).reduce((a, b) => a + ", " + b) + ".")
    }
    if (!dateRegex.matches(startDate)) {
      errors :+= SimpleError("startDate", "Start date must be of the following format \"yyyy-mm-dd\".")
    }
    if (!dateRegex.matches(endDate)) {
      errors :+= SimpleError("endDate", "End date must be of the following format \"yyyy-mm-dd\".")
    }
    if (!ectsRegex.matches(ects.toString)) {
      errors :+= SimpleError("ects", "ECTS must be a positive integer between 0 and 999.")
    }
    if (!nameRegex.matches(lecturerId)) {
      errors :+= SimpleError("lecturerId", "LecturerID must not be empty.")
    }
    if (!maxParticipantsRegex.matches(maxParticipants.toString)) {
      errors :+= SimpleError("maxParticipants", "Number of maximum participants must be a positive integer between 1 and 9999.")
    }
    if (currentParticipants < 0 || currentParticipants > maxParticipants) {
      errors :+= SimpleError("currentParticipants", "Number of current participants must be a positive integer between 0 and maximum number of participants.")
    }
    if (!CourseLanguage.All.map(_.toString).contains(courseLanguage)) {
      errors :+= SimpleError("courseLanguage", "Course Language must be one of " + CourseLanguage.All.map(_.toString).reduce((a, b) => a + ", " + b) + ".")
    }
    if (!descriptionRegex.matches(courseDescription)) {
      errors :+= SimpleError("courseDescription", "Description must contain 0 to 10000 characters.")
    }
    errors
  }
}

object Course {
  implicit val format: Format[Course] = Json.format
}
