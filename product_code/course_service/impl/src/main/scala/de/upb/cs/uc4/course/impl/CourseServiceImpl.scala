package de.upb.cs.uc4.course.impl

import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, EntityRef }
import akka.util.Timeout
import akka.{ Done, NotUsed }
import com.fasterxml.uuid.Generators
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport._
import com.lightbend.lagom.scaladsl.persistence.ReadSide
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import com.typesafe.config.Config
import de.upb.cs.uc4.authentication.model.AuthenticationRole
import de.upb.cs.uc4.course.api.CourseService
import de.upb.cs.uc4.course.impl.actor.CourseState
import de.upb.cs.uc4.course.impl.commands._
import de.upb.cs.uc4.course.impl.readside.{ CourseDatabase, CourseEventProcessor }
import de.upb.cs.uc4.course.model.Course
import de.upb.cs.uc4.shared.client.exceptions.{ UC4Exception, DetailedError, ErrorType, SimpleError }
import de.upb.cs.uc4.shared.server.ServiceCallFactory._
import de.upb.cs.uc4.shared.server.messages.{ Accepted, Confirmation, Rejected, RejectedWithError }
import de.upb.cs.uc4.user.api.UserService

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future, TimeoutException }

/** Implementation of the CourseService */
class CourseServiceImpl(
    clusterSharding: ClusterSharding, userService: UserService,
    readSide: ReadSide, processor: CourseEventProcessor, database: CourseDatabase
)(implicit ec: ExecutionContext, config: Config) extends CourseService {
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
        ))
      .map(seq => seq
        .filter { course =>
          //If courseName query is set, we check that every whitespace seperated parameter is contained
          courseName.isEmpty || courseName.get.toLowerCase.split("""\s+""").forall(course.courseName.toLowerCase.contains(_))
        }
        .filter(course => lecturerId.isEmpty || course.lecturerId == lecturerId.get))
  }

  /** @inheritdoc */
  override def addCourse(): ServiceCall[Course, Course] =
    identifiedAuthenticated(AuthenticationRole.Admin, AuthenticationRole.Lecturer) {
      (username, role) =>
        ServerServiceCall { (header, courseProposalRaw) =>
          val courseProposal = courseProposalRaw.trim
          if (role == AuthenticationRole.Lecturer && courseProposal.lecturerId != username) {
            throw UC4Exception.OwnerMismatch
          }

          // For syntax and regex checks, 5 seconds are more than enough
          val validationErrors = try {
            Await.result(courseProposal.validate, 5.seconds)
          }
          catch {
            case _: TimeoutException => throw UC4Exception.ValidationTimeout
            case e: Exception        => throw UC4Exception.InternalServerError("Validation Error", e.getMessage)
          }

          // If lecturerId is empty, the userService call cannot be found, therefore check and abort
          if (validationErrors.map(_.name).contains("lecturerId")) {
            throw new UC4Exception(422, DetailedError(ErrorType.Validation, validationErrors))
          }

          // Check if the lecturer does exist
          userService.getUser(courseProposal.lecturerId).handleRequestHeader(addAuthenticationHeader(header)).invoke().recover {
            //If the lecturer does not exist, we throw a validation error containing that info
            case ex: UC4Exception if ex.errorCode.http == 404 =>
              throw new UC4Exception(422, DetailedError(ErrorType.Validation, validationErrors :+ SimpleError("lecturerId", "Lecturer does not exist")))
          }.flatMap { _ =>
            if (validationErrors.nonEmpty) {
              throw new UC4Exception(422, DetailedError(ErrorType.Validation, validationErrors))
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
                  throw new UC4Exception(code, errorResponse)
              }
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
                throw UC4Exception.OwnerMismatch
              }
              else {
                entityRef(id).ask[Confirmation](replyTo => DeleteCourse(id, replyTo))
                  .map {
                    case Accepted => // OK
                      (ResponseHeader(200, MessageProtocol.empty, List()), Done)
                    case Rejected(reason) => // Not Found
                      throw UC4Exception.InternalServerError("Course Deletion Error", reason)
                  }
              }
            case None =>
              throw UC4Exception.NotFound
          }
        }
    }

  /** @inheritdoc */
  override def findCourseByCourseId(id: String): ServiceCall[NotUsed, Course] = authenticated(AuthenticationRole.All: _*) { _ =>
    entityRef(id).ask[Option[Course]](replyTo => commands.GetCourse(replyTo)).map {
      case Some(course) => course
      case None         => throw UC4Exception.NotFound
    }
  }

  /** @inheritdoc */
  override def updateCourse(id: String): ServiceCall[Course, Done] =
    identifiedAuthenticated(AuthenticationRole.Admin, AuthenticationRole.Lecturer) { (username, role) =>
      ServerServiceCall { (header, updatedCourseRaw) =>
        val updatedCourse = updatedCourseRaw.trim
        // Look up the sharded entity (aka the aggregate instance) for the given ID.
        if (id != updatedCourse.courseId) {
          throw UC4Exception.PathParameterMismatch
        }

        val validationErrors = try {
          Await.result(updatedCourse.validate, 5.seconds)
        }
        catch {
          case _: TimeoutException => throw UC4Exception.ValidationTimeout
            case e: Exception        => throw UC4Exception.InternalServerError("Validation Error", e.getMessage)
        }

        // If lecturerId is empty, the userService call cannot be found, therefore check and abort
        if (validationErrors.map(_.name).contains("lecturerId")) {
          throw new UC4Exception(422, DetailedError(ErrorType.Validation, validationErrors))
        }

        // Check if the lecturer does exist
        userService.getUser(updatedCourse.lecturerId).handleRequestHeader(addAuthenticationHeader(header)).invoke().recover {
          case ex: UC4Exception if ex.errorCode.http == 404 =>
            throw new UC4Exception(422, DetailedError(ErrorType.Validation, validationErrors :+ SimpleError("lecturerId", "Lecturer does not exist.")))
        }.flatMap { _ =>
          val ref = entityRef(id)

          val oldCourse = ref.ask[Option[Course]](replyTo => GetCourse(replyTo))
          oldCourse.flatMap {
            case Some(course) =>
              if (validationErrors.nonEmpty) {
                throw new UC4Exception(422, DetailedError(ErrorType.Validation, validationErrors))
              }
              if (role == AuthenticationRole.Lecturer && course.lecturerId != username) {
                throw UC4Exception.OwnerMismatch
              }
              else {
                ref.ask[Confirmation](replyTo => UpdateCourse(updatedCourse, replyTo))
                  .map {
                    case Accepted => // Update Successful
                      (ResponseHeader(200, MessageProtocol.empty, List()), Done)
                    case RejectedWithError(code, errorResponse) =>
                      throw new UC4Exception(code, errorResponse)
                  }
              }
            case None =>
              throw new UC4Exception(422, DetailedError(ErrorType.Validation, validationErrors :+ SimpleError("courseId", "CourseID does not exist.")))
          }
        }
      }
    }

  /** @inheritdoc */
  override def allowedMethodsGETPOST: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET, POST")

  /** @inheritdoc */
  override def allowedMethodsGETPUTDELETE: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET, PUT, DELETE")

  override def allowVersionNumber: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET")
}
