package de.upb.cs.uc4.hl.course.impl

import akka.util.Timeout
import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport._
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import de.upb.cs.uc4.authentication.api.AuthenticationService
import de.upb.cs.uc4.authentication.model.AuthenticationRole
import de.upb.cs.uc4.course.model.{Course, CourseLanguage, CourseType}
import de.upb.cs.uc4.hl.course.api.HlCourseService
import de.upb.cs.uc4.shared.client.{CustomException, DetailedError, SimpleError}
import de.upb.cs.uc4.shared.server.ServiceCallFactory._
import de.upb.cs.uc4.shared.server.hyperledger.HyperLedgerSession

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/** Implementation of the CourseService */
class HlCourseServiceImpl(hyperLedgerSession: HyperLedgerSession)
                         (implicit ec: ExecutionContext, auth: AuthenticationService) extends HlCourseService {

  implicit val timeout: Timeout = Timeout(5.seconds)

  /** @inheritdoc */
  override def getAllCourses(courseName: Option[String], lecturerId: Option[String]): ServerServiceCall[NotUsed, Seq[Course]] = authenticated(AuthenticationRole.All: _*) { _ =>
    hyperLedgerSession.read[Seq[Course]]("getAllCourses")
      .map(seq => seq
      .filter(course => courseName.isEmpty || course.courseName == courseName.get)
      .filter(course => lecturerId.isEmpty || course.lecturerId == lecturerId.get)
    )
  }

  /** @inheritdoc */
  override def addCourse(): ServiceCall[Course, Done] =
    identifiedAuthenticated(AuthenticationRole.Admin, AuthenticationRole.Lecturer) {
      (username, role) =>
        ServerServiceCall {
          (_,courseProposal) =>
            if (role == AuthenticationRole.Lecturer && courseProposal.lecturerId.trim != username){
              throw new CustomException(TransportErrorCode(403, 1003, "Error"), DetailedError("owner mismatch", List()))
            }
            val courseToAdd = courseProposal.copy(courseId =java.util.UUID.randomUUID().toString)
            val validationErrors = validateCourseSyntax(courseToAdd)
            if (!validationErrors.isEmpty) {
              throw new CustomException(TransportErrorCode(422, 1003, "Error"), DetailedError("validation error", validationErrors))
            }
            hyperLedgerSession.write("addCourse", courseToAdd).map((ResponseHeader(201, MessageProtocol.empty, List()), _))
        }
    }

  /** @inheritdoc */
  override def deleteCourse(id: String): ServiceCall[NotUsed, Done] =
    identifiedAuthenticated(AuthenticationRole.Admin, AuthenticationRole.Lecturer) {
      (username, role) => ServerServiceCall { _ =>
        hyperLedgerSession.read[Course]("getCourseById", id).flatMap{ course =>
          if(role == AuthenticationRole.Lecturer && username != course.lecturerId){
            throw new CustomException(TransportErrorCode(403, 1003, "Error"), DetailedError("owner mismatch",
              Seq[SimpleError](SimpleError("lecturerId", "Username must match course's lecturer."))))
          } else {
            hyperLedgerSession.write("deleteCourseById", id)
          }
        }
      }
    }

  /** @inheritdoc */
  override def findCourseByCourseId(id: String): ServiceCall[NotUsed, Course] = authenticated(AuthenticationRole.All: _*) { _ =>
    hyperLedgerSession.read[Course]("getCourseById", id)
  }

  /** @inheritdoc */
  override def updateCourse(id: String): ServiceCall[Course, Done] =
    identifiedAuthenticated(AuthenticationRole.Admin, AuthenticationRole.Lecturer) {
      (username, role) =>
        ServerServiceCall {
          (_, updatedCourse) =>
            if (id != updatedCourse.courseId) {
              throw new CustomException(TransportErrorCode(400, 1003, "Error"),
                DetailedError("path parameter mismatch", List(SimpleError("courseId", "CourseId and Id in path must match."))))
            }
            hyperLedgerSession.read[Course]("getCourseById", id).flatMap{course =>
              if(role == AuthenticationRole.Lecturer && username != course.lecturerId){
                throw new CustomException(TransportErrorCode(403, 1003, "Error"), DetailedError("owner mismatch",
                  Seq[SimpleError](SimpleError("lecturerId", "Username must match course's lecturer."))))
              } else {
                val validationErrors = validateCourseSyntax(updatedCourse)
                if (!validationErrors.isEmpty) {
                  throw new CustomException(TransportErrorCode(422, 1003, "Error"),
                    DetailedError("validation error", validationErrors))
                }
                hyperLedgerSession.write("addCourse", updatedCourse)
                  .map((ResponseHeader(201, MessageProtocol.empty, List()), _))
              }
            }
        }
    }

  /** @inheritdoc */
  override def allowedMethods: ServiceCall[NotUsed, Done] = ServerServiceCall {
    (_, _) =>
      Future.successful {
        (ResponseHeader(200, MessageProtocol.empty, List(
          ("Allow", "GET, POST, OPTIONS, PUT, DELETE"),
          ("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE")
        )), Done)
      }
  }

  /** @inheritdoc */
  override def allowedMethodsGETPOST: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET, POST")

  /** @inheritdoc */
  override def allowedMethodsGETPUTDELETE: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET, PUT, DELETE")

  /** Checks if the course attributes correspond to agreed syntax and semantics
    *
    * @param course which attributes shall be verified
    * @return response-code which gives detailed description of syntax or semantics violation
    */
  def validateCourseSyntax(course: Course): Seq[SimpleError] = {

    val nameRegex = """[\s\S]*""".r // Allowed characters for coursename "[a-zA-Z0-9\\s]+".r
    val descriptionRegex = """[\s\S]*""".r // Allowed characters  for description
    val dateRegex = """(\d\d\d\d)-(\d\d)-(\d\d)""".r

    var errors = List[SimpleError]()

    if (course.courseName == "") {
      errors :+= SimpleError("courseName", "Course name must not be empty.")
    }
    if (!(nameRegex.matches(course.courseName))) {
      errors :+= SimpleError("courseName", "Course name must only contain [..].")
    }
    if (!CourseType.All.contains(course.courseType)) {
      errors :+= (SimpleError("courseType", "Course type must be one of [Lecture, Seminar, ProjectGroup]."))
    }
    if (!dateRegex.matches(course.startDate)) {
      errors :+= (SimpleError("startDate", "Start date must be of the following format \"yyyy-mm-dd\"."))
    }
    if (!dateRegex.matches(course.endDate)) {
      errors :+= (SimpleError("endDate", "End date must be of the following format \"yyyy-mm-dd\"."))
    }
    if (course.ects <= 0) {
      errors :+= (SimpleError("ects", "Ects must be a positive integer."))
    }
    if (course.maxParticipants <= 0) {
      errors :+= (SimpleError("maxParticipants", "Maximum Participants must be a positive integer."))
    }
    if (!CourseLanguage.All.contains(course.courseLanguage)) {
      errors :+= (SimpleError("courseLanguage", "Course Language must be one of" + CourseLanguage.All+"."))
    }
    if (!descriptionRegex.matches(course.courseDescription)) {
      errors :+= SimpleError("courseDescription", "Description must only contain Strings.")
    }
    errors
  }

}
