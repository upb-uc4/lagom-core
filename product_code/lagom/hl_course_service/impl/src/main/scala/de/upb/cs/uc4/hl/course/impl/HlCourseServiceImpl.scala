package de.upb.cs.uc4.hl.course.impl

import akka.util.Timeout
import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport._
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import de.upb.cs.uc4.authentication.api.AuthenticationService
import de.upb.cs.uc4.authentication.model.AuthenticationRole
import de.upb.cs.uc4.course.model.Course
import de.upb.cs.uc4.hl.course.api.HlCourseService
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
  }

  /** @inheritdoc */
  override def addCourse(): ServiceCall[Course, Done] =
    authenticated(AuthenticationRole.Admin, AuthenticationRole.Lecturer) {
      courseProposal =>
        hyperLedgerSession.write("addCourse", courseProposal.copy(courseId = java.util.UUID.randomUUID().toString))
    }

  /** @inheritdoc */
  override def deleteCourse(id: String): ServiceCall[NotUsed, Done] =
    identifiedAuthenticated(AuthenticationRole.Admin, AuthenticationRole.Lecturer) {
      (username, role) => ServerServiceCall { _ =>
        hyperLedgerSession.read[Course]("getCourseById", id).flatMap{ course =>
          if(role == AuthenticationRole.Lecturer && username != course.lecturerId){
            throw Forbidden("You are not allowed to delete a course if another lecturer.")
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
    authenticated(AuthenticationRole.Admin, AuthenticationRole.Lecturer){ courseToChange =>
      hyperLedgerSession.write("updateCourseById", Seq(courseToChange.courseId), Seq(courseToChange))
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
}
