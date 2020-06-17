package de.upb.cs.uc4.course.impl

import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.util.Timeout
import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport._
import com.lightbend.lagom.scaladsl.persistence.ReadSide
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import de.upb.cs.uc4.authentication.api.AuthenticationService
import de.upb.cs.uc4.course.api.CourseService
import de.upb.cs.uc4.course.impl.actor.CourseState
import de.upb.cs.uc4.course.impl.commands._
import de.upb.cs.uc4.course.impl.readside.CourseEventProcessor
import de.upb.cs.uc4.course.model.Course
import de.upb.cs.uc4.shared.ServiceCallFactory._
import de.upb.cs.uc4.shared.messages.{Accepted, Confirmation, Rejected}
import de.upb.cs.uc4.user.model.Role

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/** Implementation of the CourseService */
class CourseServiceImpl(clusterSharding: ClusterSharding,
                        readSide: ReadSide, processor: CourseEventProcessor, cassandraSession: CassandraSession)
                       (implicit ec: ExecutionContext, auth: AuthenticationService) extends CourseService {
  readSide.register(processor)

  /** Looks up the entity for the given ID */
  private def entityRef(id: Long): EntityRef[CourseCommand] =
    clusterSharding.entityRefFor(CourseState.typeKey, id.toString)

  implicit val timeout: Timeout = Timeout(5.seconds)

  /** @inheritdoc */
  override def getAllCourses: ServerServiceCall[NotUsed, Seq[Course]] = authenticated(Role.All: _*){ _ =>
    cassandraSession.selectAll("SELECT id FROM courses ;")
      .map( seq => seq
        .map(row => row.getLong("id")) //Future[Seq[Long]]
        .map(entityRef(_).ask[Option[Course]](replyTo => GetCourse(replyTo))) //Future[Seq[Future[Option[Course]]]]
      )
      .flatMap(seq => Future.sequence(seq) //Future[Seq[Option[Course]]]
      .map(seq => seq
        .filter(opt => opt.isDefined) //Filter every not existing course
        .map(opt => opt.get) //Future[Seq[Course]]
      )
    )
  }

  /** @inheritdoc */
  override def addCourse(): ServiceCall[Course, Done] = authenticated(Role.Admin, Role.Lecturer)(ServerServiceCall{
    (_,courseToAdd) =>

    // Look up the sharded entity (aka the aggregate instance) for the given ID.
    val ref = entityRef(courseToAdd.courseId)

    ref.ask[Confirmation](replyTo => CreateCourse(courseToAdd, replyTo))
      .map {
        case Accepted => // Creation Successful
          (ResponseHeader(201, MessageProtocol.empty, List(("1","Operation successful"))),Done)
        case Rejected("A course with the given Id already exist.") => // Already exists
          (ResponseHeader(409,  MessageProtocol.empty, List(("1","A course with the given Id already exist."))),Done)
        case Rejected(rejectedMessage) => throwForbidden(rejectedMessage)
      }
  })

  def throwForbidden(code : String) = {
    code match {
      case ("10") =>
        throw new Forbidden(TransportErrorCode(400, 1003, "Bad Request"), new ExceptionMessage("10", "Course name must not be empty"))
      case ("11") =>
        throw new Forbidden(TransportErrorCode(400, 1003, "Bad Request"), new ExceptionMessage("11", "Course name has invalid characters"))
      //(ResponseHeader(400,  MessageProtocol.empty, List(("10","Course name must not be empty"))),Done)
      case ("20") =>
        throw new Forbidden(TransportErrorCode(400, 1003, "Bad Request"), new ExceptionMessage("20", "Course type must be one of [\"Lecture\", \"Seminar\", \"ProjectGroup\"]"))
      case ("30") =>
        throw new Forbidden(TransportErrorCode(400, 1003, "Bad Request"), new ExceptionMessage("30", "startDate must be the following format \"yyyy-mm-dd\""))
      case ("40") =>
        throw new Forbidden(TransportErrorCode(400, 1003, "Bad Request"), new ExceptionMessage("40", "endDate must be the following format \"yyyy-mm-dd\""))
      case ("50") =>
        throw new Forbidden(TransportErrorCode(400, 1003, "Bad Request"), new ExceptionMessage("50", "ects must be a positive integer number"))
      case ("60") =>
        throw new Forbidden(TransportErrorCode(400, 1003, "Bad Request"), new ExceptionMessage("60", "lecturerID unknown"))
      case ("70") =>
        throw new Forbidden(TransportErrorCode(400, 1003, "Bad Request"), new ExceptionMessage("70", "maxParticipants must be a positive integer number"))
      case ("80") =>
        throw new Forbidden(TransportErrorCode(400, 1003, "Bad Request"), new ExceptionMessage("80", "\tlanguage must be one of [\"German\", \"English\"]"))
      case ("90") =>
        throw new Forbidden(TransportErrorCode(400, 1003, "Bad Request"), new ExceptionMessage("90", "description invalid characters"))
      case (s) =>
        throw new Forbidden(TransportErrorCode(500, 1003, "Server error"), new ExceptionMessage("0", s"internal server error: $s")) // default case, should not happen
    }
  }

  /** @inheritdoc */
  override def deleteCourse(id: Long): ServiceCall[NotUsed, Done] = authenticated(Role.Admin, Role.Lecturer)(
    ServerServiceCall { (_, _) =>
    entityRef(id).ask[Confirmation](replyTo => DeleteCourse(id, replyTo))
      .map {
        case Accepted => // OK
          (ResponseHeader(200, MessageProtocol.empty, List(("1","Operation Successful"))),Done)
        case Rejected(reason) => // Not Found
          (ResponseHeader(404, MessageProtocol.empty, List(("1",reason))),Done)
      }
  })

  /** @inheritdoc */
  override def findCourseByCourseId(id: Long): ServiceCall[NotUsed, Course] = authenticated(Role.All: _*){ _ =>
    entityRef(id).ask[Option[Course]](replyTo => commands.GetCourse(replyTo)).map{
      case Some(course) => course
      case None =>  throw NotFound("ID was not found")
    }
  }

  /** @inheritdoc */
  override def findCoursesByCourseName(courseName: String): ServiceCall[NotUsed, Seq[Course]] = ServerServiceCall{
    (header, request) =>
      getAllCourses.invokeWithHeaders(header, request).map{
        case (header, response) => (header, response.filter(course => course.courseName == courseName))
      }
  }

  /** @inheritdoc */
  override def findCoursesByLecturerId(id: String): ServiceCall[NotUsed, Seq[Course]] = ServerServiceCall{
    (header, request) =>
      getAllCourses.invokeWithHeaders(header, request).map{
        case (header, response) => (header, response.filter(course => course.lecturerId == id))
      }
  }

  /** @inheritdoc */
  override def updateCourse(id : Long): ServiceCall[Course, Done] = authenticated(Role.Admin, Role.Lecturer)(ServerServiceCall {
    (_, courseToChange) =>
      // Look up the sharded entity (aka the aggregate instance) for the given ID.
      if(id != courseToChange.courseId){
        throw new Forbidden(TransportErrorCode(400, 1003, "Bad Request"), new ExceptionMessage("00", "Course ID and ID in path do not match"))
      }
      val ref = entityRef(id)

      ref.ask[Confirmation](replyTo => UpdateCourse(courseToChange, replyTo))
        .map {
          case Accepted => // OK
            (ResponseHeader(200, MessageProtocol.empty, List(("1","Operation Successful"))),Done)
          case Rejected("A course with the given Id does not exist.") => // Not Found
            (ResponseHeader(404, MessageProtocol.empty, List(("1","A course with the given Id does not exist."))),Done)
          case Rejected(rejectedMessage) => throwForbidden(rejectedMessage)
        }
  })

  /** @inheritdoc */
  override def allowedMethods: ServiceCall[NotUsed, Done] = ServerServiceCall{
    (_, _ ) =>
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
