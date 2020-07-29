package de.upb.cs.uc4.course.api

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}
import de.upb.cs.uc4.course.model.Course
import de.upb.cs.uc4.shared.client.UC4Service

object CourseService {
  val TOPIC_NAME = "Courses"
}

/** The CourseService interface.
 *
 * This describes everything that Lagom needs to know about how to serve and
 * consume the CourseService.
 */
trait CourseService extends UC4Service {
  /** Prefix for the path for the endpoints, a name/identifier for the service */
  override val pathPrefix = "/course-management"
  override val name = "course"

  /** Add a new course to the database */
  def addCourse(): ServiceCall[Course, Course]

  /** Deletes a course */
  def deleteCourse(id: String): ServiceCall[NotUsed, Done]

  /** Find courses by course ID */
  def findCourseByCourseId(id: String): ServiceCall[NotUsed, Course]

  /** Get all courses, with optional query parameters */
  def getAllCourses(courseName: Option[String], lecturerId: Option[String]): ServiceCall[NotUsed, Seq[Course]]

  /** Update an existing course */
  def updateCourse(id: String): ServiceCall[Course, Done]

  /** Allows GET, POST, PUT, DELETE */
  def allowedMethods: ServiceCall[NotUsed, Done]

  /** Allows GET POST */
  def allowedMethodsGETPOST: ServiceCall[NotUsed, Done]

  /** Allows GET PUT DELETE */
  def allowedMethodsGETPUTDELETE: ServiceCall[NotUsed, Done]

  final override def descriptor: Descriptor = {
    import Service._
    super.descriptor
      .addCalls(
        restCall(Method.GET, pathPrefix + "/courses?courseName&lecturerId", getAllCourses _),
        restCall(Method.POST, pathPrefix + "/courses", addCourse _),
        restCall(Method.PUT, pathPrefix + "/courses/:id", updateCourse _),
        restCall(Method.DELETE, pathPrefix + "/courses/:id", deleteCourse _),
        restCall(Method.GET, pathPrefix + "/courses/:id", findCourseByCourseId _),
        restCall(Method.OPTIONS, pathPrefix + "/courses", allowedMethodsGETPOST _),
        restCall(Method.OPTIONS, pathPrefix + "/courses/:id", allowedMethodsGETPUTDELETE _))
      .withAutoAcl(true)
  }
}
