package de.upb.cs.uc4.course.impl.actor


import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import akka.persistence.typed.scaladsl.{Effect, ReplyEffect}
import de.upb.cs.uc4.course.impl.CourseApplication
import de.upb.cs.uc4.course.impl.commands._
import de.upb.cs.uc4.course.impl.events.{CourseEvent, OnCourseCreate, OnCourseDelete, OnCourseUpdate}
import de.upb.cs.uc4.course.model.{Course, CourseLanguage, CourseType}
import de.upb.cs.uc4.shared.messages.{Accepted, Rejected}
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
        val responseCode = validateCourseSyntax(course)
        if (responseCode == "valid") {
          if (optCourse.isEmpty) {
            Effect.persist(OnCourseCreate(course)).thenReply(replyTo) { _ => Accepted }
          } else {
            Effect.reply(replyTo)(Rejected("A course with the given Id already exist."))
          }
        } else {
          Effect.reply(replyTo)(Rejected(responseCode))
        }


      case UpdateCourse(courseRaw, replyTo) =>

        val course = courseRaw.trim
        val responseCode = validateCourseSyntax(course)
        if (responseCode == "valid") {
          if (optCourse.isDefined) {
            Effect.persist(OnCourseUpdate(course)).thenReply(replyTo) { _ => Accepted }
          } else {
            Effect.reply(replyTo)(Rejected("A course with the given Id does not exist."))
          }
        }
        else {
          Effect.reply(replyTo)(Rejected(responseCode))
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

  def validateCourseSyntax(course: Course): String = {
    val nameRegex = "[a-zA-Z0-9\\s]+".r // Allowed characters for coursename
    val descriptionRegex = "[a-zA-Z0-9\\s]+".r // Allowed characters  for description
    val dateRegex = """(\d\d\d\d)-(\d\d)-(\d\d)""".r
    course match {

      case c if (c.courseName == "") =>
        "10" // Course name must not be empty
      case c if (!nameRegex.matches(c.courseName)) =>
        "11" // 	Course name has invalid characters
      case c if (!CourseType.All.contains(c.courseType)) =>
        "20" //Course type must be one of ["Lecture", "Seminar", "ProjectGroup"]
      //Should not happen!
      case c if (!dateRegex.matches(c.startDate)) =>
        "30" // 	startDate must be the following format "yyyy-mm-dd"
      case c if (!dateRegex.matches(c.endDate)) =>
        "40" // 	endDate must be the following format "yyyy-mm-dd"
      case c if (c.ects <= 0) =>
        "50" // 	ects must be a positive integer number
      //todo "60" 	lecturerID unknown
      case c if (c.maxParticipants <= 0) =>
        "70" // 	maxParticipants must be a positive integer number
      case c if (!CourseLanguage.All.contains(c.courseLanguage)) =>
        "80" // 	language must be one of ["German", "English"]
      case c if (!descriptionRegex.matches(c.courseDescription)) =>
        "90" // 	description invalid characters
      case _ => "valid"

    }
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
