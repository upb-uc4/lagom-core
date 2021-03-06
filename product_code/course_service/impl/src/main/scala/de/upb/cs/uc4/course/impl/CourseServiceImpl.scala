package de.upb.cs.uc4.course.impl

import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, EntityRef }
import akka.util.Timeout
import akka.{ Done, NotUsed }
import com.fasterxml.uuid.Generators
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport._
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import com.typesafe.config.Config
import de.upb.cs.uc4.authentication.model.AuthenticationRole
import de.upb.cs.uc4.course.api.CourseService
import de.upb.cs.uc4.course.impl.actor.CourseState
import de.upb.cs.uc4.course.impl.commands._
import de.upb.cs.uc4.course.impl.readside.CourseDatabase
import de.upb.cs.uc4.course.model.Course
import de.upb.cs.uc4.examreg.api.ExamregService
import de.upb.cs.uc4.shared.client.exceptions._
import de.upb.cs.uc4.shared.server.ServiceCallFactory._
import de.upb.cs.uc4.shared.server.messages.{ Accepted, Confirmation, Rejected }
import de.upb.cs.uc4.user.api.UserService
import play.api.Environment

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future, TimeoutException }

/** Implementation of the CourseService */
class CourseServiceImpl(
    clusterSharding: ClusterSharding,
    userService: UserService,
    examregService: ExamregService,
    database: CourseDatabase,
    override val environment: Environment
)(implicit ec: ExecutionContext, config: Config) extends CourseService {

  /** Looks up the entity for the given ID */
  private def entityRef(id: String): EntityRef[CourseCommand] =
    clusterSharding.entityRefFor(CourseState.typeKey, id)

  implicit val timeout: Timeout = Timeout(config.getInt("uc4.timeouts.database").milliseconds)

  lazy val validationTimeout: FiniteDuration = config.getInt("uc4.timeouts.validation").milliseconds
  lazy val internalQueryTimeout: FiniteDuration = config.getInt("uc4.timeouts.database").milliseconds

  /** @inheritdoc */
  override def getAllCourses(courseName: Option[String], lecturerId: Option[String], moduleIds: Option[String], examregNames: Option[String] = None): ServiceCall[NotUsed, Seq[Course]] = {
    ServerServiceCall { (header, _) =>
      database.getAll
        .map(seq => seq
          .map(entityRef(_).ask[Option[Course]](replyTo => GetCourse(replyTo))) //Future[Seq[Future[Option[Course]]]]
        )
        .flatMap(seq => Future.sequence(seq) //Future[Seq[Option[Course]]]
          .map(seq => seq
            .filter(opt => opt.isDefined) //Filter every not existing course
            .map(opt => opt.get) //Future[Seq[Course]]
          ))
        .map { seq =>
          seq
            .filter { course =>
              courseName match {
                case None                => true
                //If courseName query is set, we check that every whitespace separated parameter is contained
                case Some(courseNameGet) => courseNameGet.toLowerCase.split("""\s+""").forall(course.courseName.toLowerCase.contains(_))
              }
            }
            .filter { course =>
              lecturerId match {
                case None                => true
                case Some(lecturerIdGet) => course.lecturerId == lecturerIdGet
              }
            }
            .filter {
              course =>
                moduleIds match {
                  case None => true
                  case Some(listOfModuleIds) =>
                    listOfModuleIds.toLowerCase.split(',').exists(moduleId => course.moduleIds.map(_.toLowerCase).contains(moduleId.trim))
                }
            }
        }
        .flatMap { courses =>
          examregNames match {
            case None => Future.successful(courses)
            case Some(examregNameList) => examregService.getExaminationRegulations(Some(examregNameList), None).handleRequestHeader(addAuthenticationHeader(header)).invoke().map {
              examregs =>
                val moduleIdList = examregs.flatMap(_.modules).map(_.id)
                courses.filter(course => moduleIdList.exists(id => course.moduleIds.contains(id)))
            }
          }
        }.map(courses => createETagHeader(header, courses))
    }
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

          var validationErrors = try {
            Await.result(courseProposal.validate, validationTimeout)
          }
          catch {
            case _: TimeoutException => throw UC4Exception.ValidationTimeout
            case e: Exception        => throw UC4Exception.InternalServerError("Validation Error", e.getMessage)
          }

          if (courseProposal.moduleIds.nonEmpty) {
            val moduleCheckFuture = examregService.getModules(Some(courseProposal.moduleIds.mkString(",")), Some(true)).invoke().map {
              modules =>
                if (modules.isEmpty) {
                  for (index <- courseProposal.moduleIds.indices) {
                    validationErrors :+= SimpleError(s"moduleIds[$index]", "Module does not exist.")
                  }
                }
                else {
                  val moduleIdList = modules.map(_.id)
                  for (index <- courseProposal.moduleIds.indices) {
                    if (!moduleIdList.contains(courseProposal.moduleIds(index)))
                      validationErrors :+= SimpleError(s"moduleIds[$index]", "Module does not exist.")
                  }
                }
            }
            try {
              Await.result(moduleCheckFuture, internalQueryTimeout)
            }
            catch {
              case _: TimeoutException => throw UC4Exception.ValidationTimeout
              case e: Exception        => throw UC4Exception.InternalServerError("Validation Error", e.getMessage)
            }
          }

          // If lecturerId is empty, the userService call cannot be found, therefore check and abort
          if (validationErrors.map(_.name).contains("lecturerId")) {
            throw new UC4NonCriticalException(422, DetailedError(ErrorType.Validation, validationErrors))
          }

          // Check if the lecturer does exist
          userService.getUser(courseProposal.lecturerId).handleRequestHeader(addAuthenticationHeader(header)).invoke().recover {
            //If the lecturer does not exist, we throw a validation error containing that info
            case ex: UC4Exception if ex.errorCode == 404 =>
              throw new UC4NonCriticalException(422, DetailedError(ErrorType.Validation, validationErrors :+ SimpleError("lecturerId", "Lecturer does not exist")))
          }.flatMap { _ =>
            if (validationErrors.nonEmpty) {
              throw new UC4NonCriticalException(422, DetailedError(ErrorType.Validation, validationErrors))
            }
            // Generate unique ID for the course to add
            val courseToAdd = courseProposal.copy(courseId = Generators.timeBasedGenerator().generate().toString)
            // Look up the sharded entity (aka the aggregate instance) for the given ID.
            val ref = entityRef(courseToAdd.courseId)

            ref.ask[Confirmation](replyTo => CreateCourse(courseToAdd, replyTo))
              .map {
                case Accepted(_) => // Creation Successful
                  (ResponseHeader(201, MessageProtocol.empty, List(("Location", s"$pathPrefix/courses/${courseToAdd.courseId}"))), courseToAdd)
                case Rejected(code, reason) =>
                  throw UC4Exception(code, reason)
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
                    case Accepted(_) => // OK
                      (ResponseHeader(200, MessageProtocol.empty, List()), Done)
                    case Rejected(code, reason) => // Not Found
                      throw UC4Exception(code, reason)
                  }
              }
            case None =>
              throw UC4Exception.NotFound
          }
        }
    }

  /** @inheritdoc */
  override def findCourseByCourseId(id: String): ServiceCall[NotUsed, Course] = {
    ServerServiceCall { (header, _) =>
      entityRef(id).ask[Option[Course]](replyTo => commands.GetCourse(replyTo)).map {
        case Some(course) => createETagHeader(header, course)
        case None         => throw UC4Exception.NotFound
      }
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

        var validationErrors = try {
          Await.result(updatedCourse.validate, validationTimeout)
        }
        catch {
          case _: TimeoutException => throw UC4Exception.ValidationTimeout
          case e: Exception        => throw UC4Exception.InternalServerError("Validation Error", e.getMessage)
        }

        if (updatedCourse.moduleIds.nonEmpty) {
          val moduleCheckFuture = examregService.getModules(None, Some(true)).invoke().map {
            modules =>
              if (modules.isEmpty) {
                for (index <- updatedCourse.moduleIds.indices) {
                  validationErrors :+= SimpleError(s"moduleIds[$index]", "Module does not exist")
                }
              }
              else {
                val moduleIdList = modules.map(_.id)
                for (index <- updatedCourse.moduleIds.indices) {
                  if (!moduleIdList.contains(updatedCourse.moduleIds(index)))
                    validationErrors :+= SimpleError(s"moduleIds[$index]", "Module does not exist")
                }
              }
          }
          try {
            Await.result(moduleCheckFuture, internalQueryTimeout)
          }
          catch {
            case _: TimeoutException => throw UC4Exception.ValidationTimeout
            case e: Exception        => throw UC4Exception.InternalServerError("Validation Error", e.getMessage)
          }
        }

        // If lecturerId is empty, the userService call cannot be found, therefore check and abort
        if (validationErrors.map(_.name).contains("lecturerId")) {
          throw new UC4NonCriticalException(422, DetailedError(ErrorType.Validation, validationErrors))
        }

        // Check if the lecturer does exist
        userService.getUser(updatedCourse.lecturerId).handleRequestHeader(addAuthenticationHeader(header)).invoke().recover {
          case ex: UC4Exception if ex.errorCode == 404 =>
            throw new UC4NonCriticalException(422, DetailedError(ErrorType.Validation, validationErrors :+ SimpleError("lecturerId", "Lecturer does not exist.")))
        }.flatMap { _ =>
          val ref = entityRef(id)

          val oldCourse = ref.ask[Option[Course]](replyTo => GetCourse(replyTo))
          oldCourse.flatMap {
            case Some(course) =>
              if (validationErrors.nonEmpty) {
                throw new UC4NonCriticalException(422, DetailedError(ErrorType.Validation, validationErrors))
              }
              if (role == AuthenticationRole.Lecturer && course.lecturerId != username) {
                throw UC4Exception.OwnerMismatch
              }
              else {
                ref.ask[Confirmation](replyTo => UpdateCourse(updatedCourse, replyTo))
                  .map {
                    case Accepted(_) => // Update Successful
                      (ResponseHeader(200, MessageProtocol.empty, List()), Done)
                    case Rejected(code, reason) =>
                      throw UC4Exception(code, reason)
                  }
              }
            case None =>
              throw new UC4NonCriticalException(422, DetailedError(ErrorType.Validation, validationErrors :+ SimpleError("courseId", "CourseID does not exist.")))
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
