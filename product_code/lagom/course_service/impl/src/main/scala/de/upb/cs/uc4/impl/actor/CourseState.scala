package de.upb.cs.uc4.impl.actor

import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect}
import de.upb.cs.uc4.impl.commands.{CourseCommand, CreateCourse, GetAllCourses}
import de.upb.cs.uc4.impl.events.{CourseEvent, OnCourseCreate, OnGetAllCourses}
import de.upb.cs.uc4.model.Course
import de.upb.cs.uc4.shared.messages.Accepted
import play.api.libs.json.{Format, Json}

/**
  * The current state of the Aggregate.
  */
case class CourseState(Courses: List[Course]) {

  def applyCommand(cmd: CourseCommand): ReplyEffect[CourseEvent, CourseState] =
    cmd match {
      case CreateCourse(course, replyTo)  => Effect.persist(OnCourseCreate(course))
        .thenReply(replyTo) { _ => Accepted }
      case GetAllCourses(replyTo) => Effect.persist(OnGetAllCourses("meh"))
        .thenReply(replyTo)(_ => Courses)

      case _ =>
        println("Unknown Command")
        Effect.noReply
    }

  def applyEvent(evt: CourseEvent): CourseState =
    evt match {
      case OnCourseCreate(course) => println(s"Create Course : $course"); copy(Courses ++ List(course))
      case OnGetAllCourses("meh") => println("Get all Courses"); this
      case _ =>
        println("Unknown Event")
        this
    }
}

object CourseState {

  /**
    * The initial state. This is used if there is no snapshotted state to be found.
    */
  def initial: CourseState = CourseState(List())

  /**
    * The [[EventSourcedBehavior]] instances (aka Aggregates) run on sharded actors inside the Akka Cluster.
    * When sharding actors and distributing them across the cluster, each aggregate is
    * namespaced under a typekey that specifies a name and also the type of the commands
    * that sharded actor can receive.
    */
  val typeKey = EntityTypeKey[CourseCommand]("Universitycredits4Courses")

  /**
    * Format for the course state.
    *
    * Persisted entities get snapshotted every configured number of events. This
    * means the state gets stored to the database, so that when the aggregate gets
    * loaded, you don't need to replay all the events, just the ones since the
    * snapshot. Hence, a JSON format needs to be declared so that it can be
    * serialized and deserialized when storing to and from the database.
    */
  implicit val format: Format[CourseState] = Json.format
}
