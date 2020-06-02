package de.upb.cs.uc4.impl

import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.util.Timeout
import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport.{BadRequest, ExceptionMessage, TransportErrorCode}
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry
import de.upb.cs.uc4.api.CourseService
import de.upb.cs.uc4.impl.actor.CourseState
import de.upb.cs.uc4.impl.commands.{CourseCommand, CreateCourse, GetAllCourses}
import de.upb.cs.uc4.model.Course
import de.upb.cs.uc4.shared.messages.{Accepted, Confirmation}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
  * Implementation of the Universitycredits4Service.
  */
class CourseServiceImpl(
  clusterSharding: ClusterSharding,
  persistentEntityRegistry: PersistentEntityRegistry
)(implicit ec: ExecutionContext)
  extends CourseService {

  /**
    * Looks up the entity for the given ID.
    */
  private def entityRef(): EntityRef[CourseCommand] =
    clusterSharding.entityRefFor(CourseState.typeKey, "courses")

  implicit val timeout = Timeout(5.seconds)

  override def getAllCourses(): ServiceCall[NotUsed, Seq[Course]] = ServiceCall {
    _ =>

      // Look up the sharded entity (aka the aggregate instance) for the given ID.
      val ref = entityRef()

      ref.ask[Seq[Course]](replyTo => GetAllCourses(replyTo))
      //answer.map(course => course)
  }

  override def addCourse(): ServiceCall[Course, Done] = ServiceCall {
    courseToAdd =>
    // Look up the sharded entity (aka the aggregate instance) for the given ID.
    val ref = entityRef()

    ref.ask[Confirmation](replyTo => CreateCourse(courseToAdd, replyTo))
      .map {
        case Accepted => Done
        case _        => throw new BadRequest(TransportErrorCode.MethodNotAllowed, new ExceptionMessage("Nope", "Nope"), new Throwable)
      }
  }

  /*private def convertEvent(
    helloEvent: EventStreamElement[Course]
  ): api.GreetingMessageChanged = {
    helloEvent.event match {
      case GreetingMessageChanged(msg) =>
        api.GreetingMessageChanged(helloEvent.entityId, msg)
    }
  }*/
  /**
    * Deletes a course
    *
    * @param courseId Course ID to delete
    * @return void
    */
  override def deleteCourse(courseId: Long): ServiceCall[NotUsed, Done] = ???

  /**
    * Find courses by course ID
    * Find courses by course ID
    *
    * @return Seq[Course] Body Parameter  Course ID to filter by
    */
  override def findCoursesByCourseId(): ServiceCall[Int, Seq[Course]] = ???

  /**
    * Find courses by course name
    * Find courses by course name
    *
    * @return Seq[Course] Body Parameter  Course Name to filter by
    */
  override def findCoursesByCourseName(): ServiceCall[String, Seq[Course]] = ???

  /**
    * Find courses by lecturer ID
    * Find courses by lecturer with the provided ID
    *
    * @return Seq[Course] Body Parameter  Lecturer ID to filter by
    */
  override def findCoursesByLecturerId(): ServiceCall[Int, Seq[Course]] = ???

  /**
    * Find courses by lecturer name
    * Find courses by lecturer with the provided name
    *
    * @return Seq[Course] Body Parameter  Lecturer Name to filter by
    */
  override def findCoursesByLecturerName(): ServiceCall[String, Seq[Course]] = ???

  /**
    * Update an existing course
    *
    * @return void Body Parameter  Course object that needs to be added to the database
    */
  override def updateCourse(): ServiceCall[Course, Done] = ???
}
