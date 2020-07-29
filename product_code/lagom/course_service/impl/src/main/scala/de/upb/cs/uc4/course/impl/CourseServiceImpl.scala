package de.upb.cs.uc4.course.impl

import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.util.Timeout
import akka.{Done, NotUsed}
import com.fasterxml.uuid.Generators
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport._
import com.lightbend.lagom.scaladsl.persistence.ReadSide
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import de.upb.cs.uc4.authentication.api.AuthenticationService
import de.upb.cs.uc4.authentication.model.AuthenticationRole
import de.upb.cs.uc4.course.api.CourseService
import de.upb.cs.uc4.course.impl.actor.CourseState
import de.upb.cs.uc4.course.impl.commands._
import de.upb.cs.uc4.course.impl.readside.{CourseDatabase, CourseEventProcessor}
import de.upb.cs.uc4.course.model.Course
import de.upb.cs.uc4.shared.client.exceptions.{CustomException, GenericError}
import de.upb.cs.uc4.shared.server.ServiceCallFactory._
import de.upb.cs.uc4.shared.server.messages.{Accepted, Confirmation, Rejected, RejectedWithError}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/** Implementation of the CourseService */
class CourseServiceImpl(clusterSharding: ClusterSharding,
                        readSide: ReadSide, processor: CourseEventProcessor, database: CourseDatabase)
                       (implicit ec: ExecutionContext, auth: AuthenticationService) extends CourseService {
  readSide.register(processor)

  /** Looks up the entity for the given ID */
  private def entityRef(id: String): EntityRef[CourseCommand] =
    clusterSharding.entityRefFor(CourseState.typeKey, id)

  implicit val timeout: Timeout = Timeout(15.seconds)

  /** @inheritdoc */
  override def getAllCourses(courseName: Option[String], lecturerId: Option[String]): ServerServiceCall[NotUsed, Seq[Course]] = authenticated(AuthenticationRole.All: _*) { _ =>
    database.getAll
      .map(seq => seq
        .map(entityRef(_).ask[Option[Course]](replyTo => GetCourse(replyTo))) //Future[Seq[Future[Option[Course]]]]
      )
      .flatMap(seq => Future.sequence(seq) //Future[Seq[Option[Course]]]
        .map(seq => seq
          .filter(opt => opt.isDefined) //Filter every not existing course
          .map(opt => opt.get) //Future[Seq[Course]]
        )
      )
      .map(seq => seq
        .filter(course => courseName.isEmpty || course.courseName == courseName.get)
        .filter(course => lecturerId.isEmpty || course.lecturerId == lecturerId.get)
      )
  }

  /** @inheritdoc */
  override def addCourse(): ServiceCall[Course, Course] =
    identifiedAuthenticated(AuthenticationRole.Admin, AuthenticationRole.Lecturer) {
      (username, role) =>
        ServerServiceCall { (_, courseProposal) =>
          if (role == AuthenticationRole.Lecturer && courseProposal.lecturerId.trim != username){
            throw new CustomException(TransportErrorCode(403, 1003, "Error"), GenericError("owner mismatch"))
          }
          // Generate unique ID for the course to add
          val courseToAdd = courseProposal.copy(courseId = Generators.timeBasedGenerator().generate().toString)
          // Look up the sharded entity (aka the aggregate instance) for the given ID.
          val ref = entityRef(courseToAdd.courseId)

          ref.ask[Confirmation](replyTo => CreateCourse(courseToAdd, replyTo))
            .map {
              case Accepted => // Creation Successful
                (ResponseHeader(201, MessageProtocol.empty, List(("Location", s"$pathPrefix/courses/${courseToAdd.courseId}"))), courseToAdd)
              case RejectedWithError(code, errorResponse) =>
                throw new CustomException(TransportErrorCode(code, 1003, "Error"), errorResponse)
            }
        }
    }

  /** @inheritdoc */
  override def deleteCourse(id: String): ServiceCall[NotUsed, Done] =
    identifiedAuthenticated(AuthenticationRole.Admin, AuthenticationRole.Lecturer) {
      (username, role) =>
        ServerServiceCall { (_, _) =>

          entityRef(id).ask[Option[Course]](replyTo => commands.GetCourse(replyTo)).flatMap {
            case Some(course) =>
              if (role == AuthenticationRole.Lecturer && username != course.lecturerId) {
                throw new CustomException(TransportErrorCode(403, 1003, "Error"),
                  GenericError("owner mismatch"))
              } else {
                entityRef(id).ask[Confirmation](replyTo => DeleteCourse(id, replyTo))
                  .map {
                    case Accepted => // OK
                      (ResponseHeader(200, MessageProtocol.empty, List()), Done)
                    case Rejected(reason) => // Not Found
                      throw new CustomException(TransportErrorCode(500, 1003, "Error"),
                        GenericError("internal server error"))
                  }
              }
            case None =>
              throw new CustomException(TransportErrorCode(404, 1003, "Error"),
                GenericError("key not found"))
          }
        }
    }

  /** @inheritdoc */
  override def findCourseByCourseId(id: String): ServiceCall[NotUsed, Course] = authenticated(AuthenticationRole.All: _*) { _ =>
    entityRef(id).ask[Option[Course]](replyTo => commands.GetCourse(replyTo)).map {
      case Some(course) => course
      case None => throw new CustomException(TransportErrorCode(404, 1003, "Error"),
        GenericError("key not found"))
    }
  }

  /** @inheritdoc */
  override def updateCourse(id: String): ServiceCall[Course, Done] =
    identifiedAuthenticated(AuthenticationRole.Admin, AuthenticationRole.Lecturer) {
      (username, role) =>
        ServerServiceCall{
          (_, updatedCourse) =>
            // Look up the sharded entity (aka the aggregate instance) for the given ID.
            if (id != updatedCourse.courseId) {
              throw new CustomException(TransportErrorCode(400, 1003, "Error"),
                GenericError("path parameter mismatch"))
            }

            val ref = entityRef(id)

            val courseBefore = ref.ask[Option[Course]](replyTo => GetCourse(replyTo))
            courseBefore.flatMap{
              case Some(course) =>
                if(role == AuthenticationRole.Lecturer && course.lecturerId != username){
                  throw new CustomException(TransportErrorCode(403, 1003, "Error"), GenericError("owner mismatch"))
                }
                else{
                  ref.ask[Confirmation](replyTo => UpdateCourse(updatedCourse, replyTo))
                    .map {
                      case Accepted => // Update Successful
                        (ResponseHeader(200, MessageProtocol.empty, List()), Done)
                      case RejectedWithError(code, errorResponse) =>
                        throw new CustomException(TransportErrorCode(code, 1003, "Error"), errorResponse)
                    }
                }
              case None => throw new CustomException(TransportErrorCode(404, 1003, "Error"), GenericError("key not found"))
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

}
