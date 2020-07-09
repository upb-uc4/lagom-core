package de.upb.cs.uc4.course.impl.actor


import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import akka.persistence.typed.scaladsl.{Effect, ReplyEffect}
import de.upb.cs.uc4.course.impl.CourseApplication
import de.upb.cs.uc4.course.impl.commands._
import de.upb.cs.uc4.course.impl.events.{CourseEvent, OnCourseCreate, OnCourseDelete, OnCourseUpdate}
import de.upb.cs.uc4.course.model.{Course, CourseLanguage, CourseType}
import de.upb.cs.uc4.shared.client.{DetailedError, SimpleError}
import de.upb.cs.uc4.shared.server.messages.{Accepted, Rejected, RejectedWithError}
import play.api.libs.json.{Format, Json}

/** The current state of a Course */
case class CourseState(optCourse: Option[Course]) {

  /** Functions as a CommandHandler
    *
    * @param cmd the given command
    */
  def applyCommand(cmd: CourseCommand): ReplyEffect[CourseEvent, CourseState] =
    cmd match {
      case CreateCourse(courseRaw, replyTo) =>

        val course = courseRaw.trim
        val validationErrors = validateCourseSyntax(course)
        if (optCourse.isEmpty) {
          if (validationErrors.isEmpty) {
            Effect.persist(OnCourseCreate(course)).thenReply(replyTo) { _ => Accepted }
          }
          else {
            Effect.reply(replyTo)(RejectedWithError(422, DetailedError("validation error", validationErrors)))
          }
        }
        else {
          Effect.reply(replyTo)(RejectedWithError(409, DetailedError("key duplicate", List(SimpleError("courseId", "A course with the given Id already exist.")))))
        }


      case UpdateCourse(courseRaw, replyTo) =>

        val course = courseRaw.trim
        val validationErrors = validateCourseSyntax(course)
        if (optCourse.isDefined) {
          if (validationErrors.isEmpty) {
            Effect.persist(OnCourseUpdate(course)).thenReply(replyTo) { _ => Accepted }
          }
          else {
            Effect.reply(replyTo)(RejectedWithError(422, DetailedError("validation error", validationErrors)))
          }
        }
        else {
          Effect.reply(replyTo)(RejectedWithError(404, DetailedError("key not found", List(SimpleError("courseId", "Course id does not exist.")))))
        }


      case GetCourse(replyTo) =>
        Effect.reply(replyTo)(optCourse)

      case DeleteCourse(id, replyTo) =>
        if (optCourse.isDefined) {
          Effect.persist(OnCourseDelete(id)).thenReply(replyTo) { _ => Accepted }
        } else {
          Effect.reply(replyTo)(Rejected("A course with the given Id does not exist."))
        }

      case _ =>
        println("Unknown Command")
        Effect.noReply
    }

  /** Checks if the course attributes correspond to agreed syntax and semantics
    *
    * @param course which attributes shall be verified
    * @return response-code which gives detailed description of syntax or semantics violation
    */
  def validateCourseSyntax(course: Course): Seq[SimpleError] = {

    val nameRegex = """[\s\S]*""".r // Allowed characters for coursename "[a-zA-Z0-9\\s]+".r
    val descriptionRegex = """[\s\S]*""".r // Allowed characters  for description
    val dateRegex = """(\d\d\d\d)-(\d\d)-(\d\d)""".r

    var errors = List[SimpleError]()

    if (course.courseName == "") {
      errors :+= SimpleError("courseName", "Course name must not be empty.")
    }
    if (!(nameRegex.matches(course.courseName))) {
      errors :+= SimpleError("courseName", "Course name must only contain [..].")
    }
    if (!CourseType.All.contains(course.courseType)) {
      errors :+= (SimpleError("courseType", "Course type must be one of [Lecture, Seminar, ProjectGroup]."))
    }
    if (!dateRegex.matches(course.startDate)) {
      errors :+= (SimpleError("startDate", "Start date must be of the following format \"yyyy-mm-dd\"."))
    }
    if (!dateRegex.matches(course.endDate)) {
      errors :+= (SimpleError("endDate", "End date must be of the following format \"yyyy-mm-dd\"."))
    }
    if (course.ects <= 0) {
      errors :+= (SimpleError("ects", "Ects must be a positive integer."))
    }
    if (course.maxParticipants <= 0) {
      errors :+= (SimpleError("maxParticipants", "Maximum Participants must be a positive integer."))
    }
    if (!CourseLanguage.All.contains(course.courseLanguage)) {
      errors :+= (SimpleError("courseLanguage", "Course Language must be one of" + CourseLanguage.All+"."))
    }
    if (!descriptionRegex.matches(course.courseDescription)) {
      errors :+= SimpleError("courseDescription", "Description must only contain Strings.")
    }
    errors
  }


  /** Functions as an EventHandler
    *
    * @param evt the given event
    */
  def applyEvent(evt: CourseEvent): CourseState =
    evt match {
      case OnCourseCreate(course) => copy(Some(course))
      case OnCourseUpdate(course) => copy(Some(course))
      case OnCourseDelete(_) => copy(None)
      case _ =>
        println("Unknown Event")
        this
    }
}

object CourseState {

  /**
    * The initial state. This is used if there is no snapshotted state to be found.
    */
  def initial: CourseState = CourseState(None)

  /**
    * The [[akka.persistence.typed.scaladsl.EventSourcedBehavior]] instances (aka Aggregates) run on sharded actors inside the Akka Cluster.
    * When sharding actors and distributing them across the cluster, each aggregate is
    * namespaced under a typekey that specifies a name and also the type of the commands
    * that sharded actor can receive.
    */
  val typeKey: EntityTypeKey[CourseCommand] = EntityTypeKey[CourseCommand](CourseApplication.cassandraOffset)

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
