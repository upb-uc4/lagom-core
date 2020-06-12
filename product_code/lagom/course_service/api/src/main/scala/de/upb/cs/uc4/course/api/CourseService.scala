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

  /** Add a new course to the database */
  def addCourse(): ServiceCall[Course, Done]

  /** Deletes a course */
  def deleteCourse(id: Long): ServiceCall[NotUsed, Done]

  /**  Find courses by course ID */
  def findCourseByCourseId(id: Long): ServiceCall[NotUsed, Course]

  /** Find courses by course name */
  def findCoursesByCourseName(name: String): ServiceCall[NotUsed, Seq[Course]]

  /** Find courses by lecturer with the provided ID */
  def findCoursesByLecturerId(id: Long): ServiceCall[NotUsed, Seq[Course]]

  /** Get all courses */
  def getAllCourses: ServiceCall[NotUsed, Seq[Course]]

  /** Update an existing course */
  def updateCourse(): ServiceCall[Course, Done]

  /** Allows GET, POST, PUT, DELETE */
  def allowedMethods: ServiceCall[NotUsed, Done]

  final override def descriptor: Descriptor = {
    import Service._
    named("course").withCalls(
      restCall(Method.POST, "/course", addCourse _),
      restCall(Method.DELETE, "/course?id", deleteCourse _),
      restCall(Method.GET, "/course/findByCourseId?id", findCourseByCourseId _),
      restCall(Method.GET, "/course/findByCourseName?name", findCoursesByCourseName _),
      restCall(Method.GET, "/course/findByLecturerId?id", findCoursesByLecturerId _),
      restCall(Method.GET, "/course", getAllCourses _),
      restCall(Method.PUT, "/course", updateCourse _),
      restCall(Method.OPTIONS, "/course", allowedMethods _)
    ).withAutoAcl(true)
  }
}
