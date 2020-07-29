package de.upb.cs.uc4.course.model

import de.upb.cs.uc4.shared.client.exceptions.SimpleError
import play.api.libs.json._

case class Course(
                   courseId: String,
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
                 )
{
  def trim: Course = {
    copy(
      courseId,
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
  def validateCourseSyntax: Seq[SimpleError] = {

    val nameRegex = """[\s\S]{1,100}""".r // Allowed characters for coursename: 1-100 of everything
    val descriptionRegex = """[\s\S]{0,10000}""".r // Allowed characters  for description
    val dateRegex = """^(?:(?:(?:(?:(?:[1-9]\d)(?:0[48]|[2468][048]|[13579][26])|(?:(?:[2468][048]|[13579][26])00))(-)(?:0?2\1(?:29)))|(?:(?:[1-9]\d{3})(-)(?:(?:(?:0?[13578]|1[02])\2(?:31))|(?:(?:0?[13-9]|1[0-2])\2(?:29|30))|(?:(?:0?[1-9])|(?:1[0-2]))\2(?:0?[1-9]|1\d|2[0-8])))))$""".r


    var errors = List[SimpleError]()


    courseName match {
      case "" => errors :+= SimpleError("courseName", "Course name must not be empty.")
      case _ if(!(nameRegex.matches(courseName))) => errors :+= SimpleError("courseName", "Course name must not contain more than 100 characters.")
      case _ =>
    }
    if (!CourseType.All.map(_.toString).contains(courseType)) {
      errors :+= SimpleError("courseType", "Course type must be one of " + CourseType.All.map(_.toString).reduce((a,b) => a+", "+b) + ".")
    }
    if (!dateRegex.matches(startDate)) {
      errors :+= SimpleError("startDate", "Start date must be of the following format \"yyyy-mm-dd\".")
    }
    if (!dateRegex.matches(endDate)) {
      errors :+= SimpleError("endDate", "End date must be of the following format \"yyyy-mm-dd\".")
    }
    if (ects <= 0 || ects > 1000) {
      errors :+= SimpleError("ects", "ECTS must be a positive integer between 1 and 999.")
    }
    if (maxParticipants <= 0 || maxParticipants > 10000) {
      errors :+= SimpleError("maxParticipants", "Number of maximum participants must be a positive integer between 1 and 9999.")
    }
    if (currentParticipants < 0 || currentParticipants > maxParticipants) {
      errors :+= SimpleError("currentParticipants", "Number of current participants must be a positive integer between 0 and maximum number of participants.")
    }
    if (!CourseLanguage.All.map(_.toString).contains(courseLanguage)) {
      errors :+= SimpleError("courseLanguage", "Course Language must be one of " + CourseLanguage.All.map(_.toString).reduce((a,b) => a+", "+b) + ".")
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
