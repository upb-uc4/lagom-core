package de.upb.cs.uc4.api

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}
import de.upb.cs.uc4.model.Course

object CourseService  {
  val TOPIC_NAME = "Courses"
}

/**
  * The UniversityCredits4 service interface.
  * <p>
  * This describes everything that Lagom needs to know about how to serve and
  * consume the Universitycredits4Service.
  */
trait CourseService extends Service {

  /**
    * Add a new course to the database
    *
    * @return void Body Parameter  Course object that needs to be added to the database
    */
  def addCourse(): ServiceCall[Course, Done]


  // apiKey:String  -- not yet supported header params
  /**
    * Deletes a course
    *
    * @param courseId Course ID to delete
    * @return void
    */
  def deleteCourse(courseId: Long): ServiceCall[NotUsed, Done]

  /**
    * Find courses by course ID
    * Find courses by course ID
    *
    * @return Seq[Course] Body Parameter  Course ID to filter by
    */
  def findCoursesByCourseId(): ServiceCall[Int, Seq[Course]]

  /**
    * Find courses by course name
    * Find courses by course name
    *
    * @return Seq[Course] Body Parameter  Course Name to filter by
    */
  def findCoursesByCourseName(): ServiceCall[String, Seq[Course]]

  /**
    * Find courses by lecturer ID
    * Find courses by lecturer with the provided ID
    *
    * @return Seq[Course] Body Parameter  Lecturer ID to filter by
    */
  def findCoursesByLecturerId(): ServiceCall[Int, Seq[Course]]

  /**
    * Find courses by lecturer name
    * Find courses by lecturer with the provided name
    *
    * @return Seq[Course] Body Parameter  Lecturer Name to filter by
    */
  def findCoursesByLecturerName(): ServiceCall[String, Seq[Course]]

  /**
    * Get all courses
    * Returns all courses
    *
    * @return Seq[Course]
    */
  def getAllCourses: ServiceCall[NotUsed, Seq[Course]]

  /**
    * Update an existing course
    *
    * @return void Body Parameter  Course object that needs to be added to the database
    */
  def updateCourse(): ServiceCall[Course, Done]

  final override def descriptor: Descriptor = {
    import Service._
    named("CourseApi").withCalls(
      restCall(Method.POST, "/course", addCourse _),
      restCall(Method.DELETE, "/course", deleteCourse _),
      restCall(Method.GET, "/course/findByCourseID", findCoursesByCourseId _),
      restCall(Method.GET, "/course/findByCourseName", findCoursesByCourseName _),
      restCall(Method.GET, "/course/findByLecturerID", findCoursesByLecturerId _),
      restCall(Method.GET, "/course/findByLecturerName", findCoursesByLecturerName _),
      restCall(Method.GET, "/course", getAllCourses _),
      restCall(Method.PUT, "/course", updateCourse _)
    ).withAutoAcl(true)
  }
}

/*
  * The greeting message class.

case class GreetingMessage(message: String)

object GreetingMessage {
  /**
    * Format for converting greeting messages to and from JSON.
    *
    * This will be picked up by a Lagom implicit conversion from Play's JSON format to Lagom's message serializer.
    */
  implicit val format: Format[GreetingMessage] = Json.format[GreetingMessage]
}



/**
  * The greeting message class used by the topic stream.
  * Different than [[GreetingMessage]], this message includes the name (id).
  */
case class GreetingMessageChanged(name: String, message: String)

object GreetingMessageChanged {
  /**
    * Format for converting greeting messages to and from JSON.
    *
    * This will be picked up by a Lagom implicit conversion from Play's JSON format to Lagom's message serializer.
    */
  implicit val format: Format[GreetingMessageChanged] = Json.format[GreetingMessageChanged]
}*/
