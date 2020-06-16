package de.upb.cs.uc4.course.api

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}
import de.upb.cs.uc4.course.model.Course

object CourseService  {
  val TOPIC_NAME = "Courses"
}

/** The CourseService interface.
  *
  * This describes everything that Lagom needs to know about how to serve and
  * consume the CourseService.
  */
trait CourseService extends Service {
  /** Prefix for the path for the endpoints, a name/identifier for the service*/
  val pathPrefix = "/course-management"

  /** Add a new course to the database */
  def addCourse(): ServiceCall[Course, Done]

  /** Deletes a course */
  def deleteCourse(id: Long): ServiceCall[NotUsed, Done]

  /**  Find courses by course ID */
  def findCourseByCourseId(id: Long): ServiceCall[NotUsed, Course]

  /** Find courses by course name */
  def findCoursesByCourseName(name: String): ServiceCall[NotUsed, Seq[Course]]

  /** Find courses by lecturer with the provided ID */
  def findCoursesByLecturerId(id: String): ServiceCall[NotUsed, Seq[Course]]

  /** Get all courses */
  def getAllCourses: ServiceCall[NotUsed, Seq[Course]]

  /** Update an existing course */
  def updateCourse(id: Long): ServiceCall[Course, Done]

  /** Allows GET, POST, PUT, DELETE */
  def allowedMethods: ServiceCall[NotUsed, Done]

  /** Allows GET POST*/
  def allowedMethodsGETPOST: ServiceCall[NotUsed, Done]

  /** Allows GET PUT DELETE*/
  def allowedMethodsGETPUTDELETE: ServiceCall[NotUsed, Done]

  final override def descriptor: Descriptor = {
    import Service._
    named("CourseApi").withCalls(
      restCall(Method.GET, pathPrefix + "/courses", getAllCourses _),
      restCall(Method.POST, pathPrefix + "/courses", addCourse _),
      restCall(Method.PUT, pathPrefix + "/courses/:id", updateCourse _),
      restCall(Method.DELETE, pathPrefix + "/courses/:id", deleteCourse _),
      restCall(Method.GET, pathPrefix + "/courses/:id", findCourseByCourseId _),
      restCall(Method.GET, pathPrefix + "/courses?courseName", findCoursesByCourseName _),
      restCall(Method.OPTIONS, pathPrefix + "/courses", allowedMethodsGETPOST _),
      restCall(Method.OPTIONS, pathPrefix + "/courses/:id", allowedMethodsGETPUTDELETE _)
    ).withAutoAcl(true)
  }
}
