package de.upb.cs.uc4.impl

import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.util.Timeout
import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport.{MessageProtocol, NotFound, ResponseHeader}
import com.lightbend.lagom.scaladsl.persistence.ReadSide
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import de.upb.cs.uc4.api.CourseService
import de.upb.cs.uc4.impl.actor.CourseState
import de.upb.cs.uc4.impl.commands._
import de.upb.cs.uc4.impl.readside.CourseEventProcessor
import de.upb.cs.uc4.model.Course
import de.upb.cs.uc4.shared.messages.{Accepted, Confirmation, Rejected}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/**
  * Implementation of the Universitycredits4Service.
  */
class CourseServiceImpl(clusterSharding: ClusterSharding,
                        readSide: ReadSide, processor: CourseEventProcessor, cassandraSession: CassandraSession)
                       (implicit ec: ExecutionContext) extends CourseService {
  readSide.register(processor)

  /**
    * Looks up the entity for the given ID.
    */
  private def entityRef(id: Long): EntityRef[CourseCommand] =
    clusterSharding.entityRefFor(CourseState.typeKey, id.toString)

  implicit val timeout: Timeout = Timeout(5.seconds)

  /** @inheritdoc */
  override def getAllCourses: ServiceCall[NotUsed, Seq[Course]] = ServiceCall{ _ =>


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
  override def addCourse(): ServiceCall[Course, Done] = ServerServiceCall {
    (_,courseToAdd) =>

    // Look up the sharded entity (aka the aggregate instance) for the given ID.
    val ref = entityRef(courseToAdd.courseId)

    ref.ask[Confirmation](replyTo => CreateCourse(courseToAdd, replyTo))
      .map {
        case Accepted => // Creation Successful
          (ResponseHeader(201, MessageProtocol.empty, List(("1","Operation successful"))),Done)
        case Rejected(reason) => // Already exists
          (ResponseHeader(409,  MessageProtocol.empty, List(("1",reason))),Done)
      }

  }

  /** @inheritdoc */
  override def deleteCourse(): ServiceCall[Long, Done] = ServerServiceCall { (_, id) =>
    entityRef(id).ask[Confirmation](replyTo => DeleteCourse(id, replyTo))
      .map {
        case Accepted => // OK
          (ResponseHeader(200, MessageProtocol.empty, List(("1","Operation Successful"))),Done)
        case Rejected(reason) => // Not Found
          (ResponseHeader(404, MessageProtocol.empty, List(("1",reason))),Done)
      }
  }

  /** @inheritdoc */
  override def findCourseByCourseId(): ServiceCall[Long, Course] = ServiceCall{ id =>
    entityRef(id).ask[Option[Course]](replyTo => GetCourse(replyTo)).map{
      case Some(course) => course
      case None =>  throw NotFound("ID was not found")
    }
  }


  /** @inheritdoc */
  override def findCoursesByCourseName(): ServiceCall[String, Seq[Course]] = ServiceCall{ name =>
    getAllCourses.invoke().map(_.filter(course => course.courseName == name))
  }

  /** @inheritdoc */
  override def findCoursesByLecturerId(): ServiceCall[Long, Seq[Course]] = ServiceCall{ lecturerId =>
    getAllCourses.invoke().map(_.filter(course => course.lecturerId == lecturerId))
  }

  /** @inheritdoc */
  override def updateCourse(): ServiceCall[Course, Done] = ServerServiceCall {
    (responseHeader,courseToChange) =>
      // Look up the sharded entity (aka the aggregate instance) for the given ID.
      val ref = entityRef(courseToChange.courseId)

      ref.ask[Confirmation](replyTo => UpdateCourse(courseToChange, replyTo))
        .map {
          case Accepted => // OK
            (ResponseHeader(200, MessageProtocol.empty, List(("1","Operation Successful"))),Done)
          case Rejected(reason) => // Not Found
            (ResponseHeader(404, MessageProtocol.empty, List(("1",reason))),Done)
        }
  }

  override def allowedMethods: ServiceCall[NotUsed, Done] = ServerServiceCall{
    (_, _ ) =>
      Future.successful {
        (ResponseHeader(200, MessageProtocol.empty, List(
          ("Allow", "GET, POST, OPTIONS, PUT"),
          ("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT")
        )), Done)
      }
  }
}
