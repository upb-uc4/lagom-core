package de.upb.cs.uc4.hl.course.impl

import akka.util.Timeout
import akka.{Done, NotUsed}
import com.fasterxml.uuid.Generators
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport._
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import de.upb.cs.uc4.authentication.api.AuthenticationService
import de.upb.cs.uc4.authentication.model.AuthenticationRole
import de.upb.cs.uc4.course.model.Course
import de.upb.cs.uc4.hl.course.api.HlCourseService
import de.upb.cs.uc4.shared.client.exceptions.{CustomException, DetailedError, GenericError}
import de.upb.cs.uc4.shared.server.ServiceCallFactory._
import de.upb.cs.uc4.shared.server.hyperledger.HyperLedgerSession

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/** Implementation of the CourseService */
class HlCourseServiceImpl(hyperLedgerSession: HyperLedgerSession)
                         (implicit ec: ExecutionContext, auth: AuthenticationService) extends HlCourseService {

  implicit val timeout: Timeout = Timeout(60.seconds)

  /** @inheritdoc */
  override def getAllCourses(courseName: Option[String], lecturerId: Option[String]): ServerServiceCall[NotUsed, Seq[Course]] = authenticated(AuthenticationRole.All: _*) { _ =>
    hyperLedgerSession.read[Seq[Course]]("getAllCourses")
      .map(seq => seq
      .filter(course => courseName.isEmpty || course.courseName == courseName.get)
      .filter(course => lecturerId.isEmpty || course.lecturerId == lecturerId.get)
    )
  }

  /** @inheritdoc */
  override def addCourse(): ServiceCall[Course, Course] =
    identifiedAuthenticated(AuthenticationRole.Admin, AuthenticationRole.Lecturer) {
      (username, role) =>
        ServerServiceCall {
          (_,courseProposal) =>
            if (role == AuthenticationRole.Lecturer && courseProposal.lecturerId.trim != username){
              throw new CustomException(TransportErrorCode(403, 1003, "Error"), GenericError("owner mismatch"))
            }
            val courseToAdd = courseProposal.copy(courseId = Generators.timeBasedGenerator().generate().toString)
            val validationErrors = courseToAdd.validateCourseSyntax
            if (validationErrors.nonEmpty) {
              throw new CustomException(TransportErrorCode(422, 1003, "Error"), DetailedError("validation error", validationErrors))
            }
            hyperLedgerSession.write("addCourse", courseToAdd).map{
              case Done => // Creation Successful
                (ResponseHeader(201, MessageProtocol.empty, List(("Location", s"$pathPrefix/courses/${courseToAdd.courseId}"))), courseToAdd)
            }
        }
    }

  /** @inheritdoc */
  override def deleteCourse(id: String): ServiceCall[NotUsed, Done] =
    identifiedAuthenticated(AuthenticationRole.Admin, AuthenticationRole.Lecturer) {
      (username, role) => ServerServiceCall { _ =>
        hyperLedgerSession.read[Course]("getCourseById", id).flatMap{ course =>
          if(role == AuthenticationRole.Lecturer && username != course.lecturerId){
            throw new CustomException(TransportErrorCode(403, 1003, "Error"), GenericError("owner mismatch"))
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
                GenericError("path parameter mismatch"))
            }
            hyperLedgerSession.read[Course]("getCourseById", id).flatMap{course =>
              if(role == AuthenticationRole.Lecturer && username != course.lecturerId){
                throw new CustomException(TransportErrorCode(403, 1003, "Error"), GenericError("owner mismatch"))
              } else {
                val validationErrors = updatedCourse.validateCourseSyntax
                if (validationErrors.nonEmpty) {
                  throw new CustomException(TransportErrorCode(422, 1003, "Error"),
                    DetailedError("validation error", validationErrors))
                }
                hyperLedgerSession.write("updateCourseById", Seq(updatedCourse.courseId), Seq(updatedCourse))
                  .map((ResponseHeader(200, MessageProtocol.empty, List()), _))
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

  /** This Methods needs to allow a GET-Method */
  override def allowVersionNumber: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET")
}
