package de.upb.cs.uc4.course.impl.actor

import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import akka.persistence.typed.scaladsl.{ Effect, ReplyEffect }
import de.upb.cs.uc4.course.impl.CourseApplication
import de.upb.cs.uc4.course.impl.commands._
import de.upb.cs.uc4.course.impl.events.{ CourseEvent, OnCourseCreate, OnCourseDelete, OnCourseUpdate }
import de.upb.cs.uc4.course.model.Course
import de.upb.cs.uc4.shared.client.exceptions.{ ErrorType, GenericError }
import de.upb.cs.uc4.shared.server.messages.{ Accepted, Rejected }
import play.api.libs.json.{ Format, Json }

/** The current state of a Course */
case class CourseState(optCourse: Option[Course]) {

  /** Functions as a CommandHandler
    *
    * @param cmd the given command
    */
  def applyCommand(cmd: CourseCommand): ReplyEffect[CourseEvent, CourseState] =
    cmd match {
      case CreateCourse(course, replyTo) =>

        if (optCourse.isEmpty) {
          Effect.persist(OnCourseCreate(course)).thenReply(replyTo) { _ => Accepted }
        }
        else {
          Effect.reply(replyTo)(Rejected(500, GenericError(ErrorType.InternalServer)))
        }

      case UpdateCourse(course, replyTo) =>
        if (optCourse.isDefined) {
          Effect.persist(OnCourseUpdate(course)).thenReply(replyTo) { _ => Accepted }
        }
        else {
          Effect.reply(replyTo)(Rejected(500, GenericError(ErrorType.InternalServer)))
        }

      case GetCourse(replyTo) =>
        Effect.reply(replyTo)(optCourse)

      case DeleteCourse(id, replyTo) =>
        if (optCourse.isDefined) {
          Effect.persist(OnCourseDelete(id)).thenReply(replyTo) { _ => Accepted }
        }
        else {
          Effect.reply(replyTo)(Rejected(500, GenericError(ErrorType.InternalServer)))
        }

      case _ =>
        println("Unknown Command")
        Effect.noReply
    }

  /** Functions as an EventHandler
    *
    * @param evt the given event
    */
  def applyEvent(evt: CourseEvent): CourseState =
    evt match {
      case OnCourseCreate(course) => copy(Some(course))
      case OnCourseUpdate(course) => copy(Some(course))
      case OnCourseDelete(_)      => copy(None)
      case _ =>
        println("Unknown Event")
        this
    }
}

object CourseState {

  /** The initial state. This is used if there is no snapshotted state to be found.
    */
  def initial: CourseState = CourseState(None)

  /** The [[akka.persistence.typed.scaladsl.EventSourcedBehavior]] instances (aka Aggregates) run on sharded actors inside the Akka Cluster.
    * When sharding actors and distributing them across the cluster, each aggregate is
    * namespaced under a typekey that specifies a name and also the type of the commands
    * that sharded actor can receive.
    */
  val typeKey: EntityTypeKey[CourseCommand] = EntityTypeKey[CourseCommand](CourseApplication.offset)

  /** Format for the course state.
    *
    * Persisted entities get snapshotted every configured number of events. This
    * means the state gets stored to the database, so that when the aggregate gets
    * loaded, you don't need to replay all the events, just the ones since the
    * snapshot. Hence, a JSON format needs to be declared so that it can be
    * serialized and deserialized when storing to and from the database.
    */
  implicit val format: Format[CourseState] = Json.format
}
