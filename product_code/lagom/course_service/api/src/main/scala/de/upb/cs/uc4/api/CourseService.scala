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
    * @return void
    */
  def deleteCourse(): ServiceCall[Long, Done]

  /**
    * Find courses by course ID
    * Find courses by course ID
    *
    * @return Seq[Course] Body Parameter  Course ID to filter by
    */
  def findCourseByCourseId(): ServiceCall[Long, Course]

  /**
    * Find courses by course name
    * Find courses by course name
    *
    * @return Source[Course, NotUsed]
    */
  //def findCoursesByCourseName(): ServiceCall[String, Source[Course, NotUsed]]
  def findCoursesByCourseName(): ServiceCall[String, Seq[Course]]

  /**
    * Find courses by lecturer ID
    * Find courses by lecturer with the provided ID
    *
    * @return Seq[Course] Body Parameter  Lecturer ID to filter by
    */
  //def findCoursesByLecturerId(): ServiceCall[Long, Source[Course, NotUsed]]
  def findCoursesByLecturerId(): ServiceCall[Long, Seq[Course]]

  /**
    * Get all courses
    * Returns all courses
    *
    * @return Seq[Course]
    */
  //def getAllCourses: ServiceCall[NotUsed, Source[Course, NotUsed]]
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
      restCall(Method.GET, "/course/findByCourseID", findCourseByCourseId _),
      restCall(Method.GET, "/course/findByCourseName", findCoursesByCourseName _),
      restCall(Method.GET, "/course/findByLecturerID", findCoursesByLecturerId _),
      restCall(Method.GET, "/course", getAllCourses _),
      restCall(Method.PUT, "/course", updateCourse _)
    ).withAutoAcl(true)
  }
}
