package de.upb.cs.uc4.course

import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.ServiceCall
import de.upb.cs.uc4.course.api.CourseService
import de.upb.cs.uc4.course.model.Course
import de.upb.cs.uc4.shared.client.exceptions.UC4Exception

import scala.concurrent.Future

class CourseServiceStub extends CourseService with DefaultTestCourses {

  private var courses: Seq[Course] = Seq()

  def resetToDefaults(): Unit = {
    courses = Seq(course0, course0, course0, course0)
  }
  def resetToEmpty(): Unit = {
    courses = Seq()
  }
  /** Add a new course to the database */
  override def addCourse(): ServiceCall[Course, Course] = ServiceCall {
    course =>
      courses :+= course
      Future.successful(course)
  }

  /** Deletes a course */
  override def deleteCourse(id: String): ServiceCall[NotUsed, Done] = ServiceCall {
    _ =>
      courses = courses.filter(_.courseId != id)
      Future.successful(Done)
  }

  /** Find courses by course ID */
  override def findCourseByCourseId(id: String): ServiceCall[NotUsed, Course] = ServiceCall {
    _ =>
      val optCourse = courses.find(course => course.courseId == id)
      if (optCourse.isDefined) {
        Future.successful(optCourse.get)
      }
      else {
        Future.failed(UC4Exception.NotFound)
      }
  }

  /** Get all courses, with optional query parameters */
  override def getAllCourses(courseName: Option[String], lecturerId: Option[String]): ServiceCall[NotUsed, Seq[Course]] = ServiceCall {
    _ =>
      Future.successful(
        courses.filter { course =>
          courseName.isEmpty || courseName.get.toLowerCase.split("""\s+""").forall(course.courseName.toLowerCase.contains(_))
        }.filter { course =>
          lecturerId.isEmpty || course.lecturerId == lecturerId.get
        }
      )
  }

  /** Update an existing course */
  override def updateCourse(id: String): ServiceCall[Course, Done] = ServiceCall {
    updatedCourse =>
      if (!courses.exists(_.courseId == id)) {
        Future.failed(UC4Exception.NotFound)
      }
      courses = courses.filter(_.courseId != id)
      courses :+= updatedCourse
      Future.successful(Done)
  }

  /** Allows GET POST */
  override def allowedMethodsGETPOST: ServiceCall[NotUsed, Done] = ServiceCall {
    _ => Future.successful(Done)
  }

  /** Allows GET PUT DELETE */
  override def allowedMethodsGETPUTDELETE: ServiceCall[NotUsed, Done] = ServiceCall {
    _ => Future.successful(Done)
  }

  /** This Methods needs to allow a GET-Method */
  override def allowVersionNumber: ServiceCall[NotUsed, Done] = ServiceCall {
    _ => Future.successful(Done)
  }
}
