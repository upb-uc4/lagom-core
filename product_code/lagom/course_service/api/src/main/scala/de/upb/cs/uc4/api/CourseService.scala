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
  *
  * This describes everything that Lagom needs to know about how to serve and
  * consume the Universitycredits4Service.
  */
trait CourseService extends Service {

  /** Add a new course to the database */
  def addCourse(): ServiceCall[Course, Done]

  /** Deletes a course */
  def deleteCourse(): ServiceCall[Long, Done]

  /**  Find courses by course ID */
  def findCourseByCourseId(): ServiceCall[Long, Course]

  /** Find courses by course name */
  def findCoursesByCourseName(): ServiceCall[String, Seq[Course]]

  /** Find courses by lecturer with the provided ID */
  def findCoursesByLecturerId(): ServiceCall[Long, Seq[Course]]

  /** Get all courses */
  def getAllCourses: ServiceCall[NotUsed, Seq[Course]]

  /** Update an existing course */
  def updateCourse(): ServiceCall[Course, Done]

  /** Allows GET, POST, PUT, DELETE */
  def allowedMethods: ServiceCall[NotUsed, Done]

  final override def descriptor: Descriptor = {
    import Service._
    named("CourseApi").withCalls(
      restCall(Method.POST, "/course", addCourse _),
      restCall(Method.DELETE, "/course", deleteCourse _),
      restCall(Method.GET, "/course/findByCourseID", findCourseByCourseId _),
      restCall(Method.GET, "/course/findByCourseName", findCoursesByCourseName _),
      restCall(Method.GET, "/course/findByLecturerID", findCoursesByLecturerId _),
      restCall(Method.GET, "/course", getAllCourses _),
      restCall(Method.PUT, "/course", updateCourse _),
      restCall(Method.OPTIONS, "/course", allowedMethods _)
    ).withAutoAcl(true)
  }
}
