package de.upb.cs.uc4.course.impl

import java.util.Base64

import akka.Done
import com.lightbend.lagom.scaladsl.api.transport.RequestHeader
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import de.upb.cs.uc4.authentication.AuthenticationServiceStub
import de.upb.cs.uc4.authentication.api.AuthenticationService
import de.upb.cs.uc4.course.api.CourseService
import de.upb.cs.uc4.course.model.{ Course, CourseLanguage, CourseType }
import de.upb.cs.uc4.shared.client.exceptions.CustomException
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{ Minutes, Seconds, Span }
import org.scalatest.wordspec.AsyncWordSpec

/** Tests for the CourseService
  * All tests need to be started in the defined order
  */
class CourseServiceSpec extends AsyncWordSpec with Matchers with BeforeAndAfterAll with Eventually {

  private val server = ServiceTest.startServer(
    ServiceTest.defaultSetup
      .withJdbc()
  ) { ctx =>
      new CourseApplication(ctx) with LocalServiceLocator {
        override lazy val authenticationService: AuthenticationService = new AuthenticationServiceStub
      }
    }

  val client: CourseService = server.serviceClient.implement[CourseService]

  //Test courses
  var course0: Course = Course("", "Course 0", CourseType.Lecture.toString, "2020-04-11", "2020-08-01", 8, "11", 60, 20, CourseLanguage.German.toString, "A test")
  var course1: Course = Course("", "Course 1", CourseType.Lecture.toString, "2020-04-11", "2020-08-01", 8, "11", 60, 20, CourseLanguage.German.toString, "A test")
  var course2: Course = Course("", "Course 1", CourseType.Lecture.toString, "2020-04-11", "2020-08-01", 8, "12", 60, 20, CourseLanguage.German.toString, "A test")
  var course3: Course = Course("", "Course 3", CourseType.Lecture.toString, "2020-04-11", "2020-08-01", 8, "11", 60, 20, CourseLanguage.German.toString, "A test")

  override protected def afterAll(): Unit = server.stop()

  def addAuthorizationHeader(): RequestHeader => RequestHeader = { header =>
    header.withHeader("Authorization", "Basic " + Base64.getEncoder.encodeToString("MOCK:MOCK".getBytes()))
  }

  /** Tests only working if the whole instance is started */
  "CourseService service" should {

    "get all courses with no courses" in {
      client.getAllCourses(None, None).handleRequestHeader(addAuthorizationHeader()).invoke().map { answer =>
        answer shouldBe empty
      }
    }

    "create three courses" in {
      client.addCourse().handleRequestHeader(addAuthorizationHeader()).invoke(course0).map {
        answer => course0 = answer
      }
      client.addCourse().handleRequestHeader(addAuthorizationHeader()).invoke(course1).map {
        answer => course1 = answer
      }
      client.addCourse().handleRequestHeader(addAuthorizationHeader()).invoke(course2).map {
        answer => course2 = answer
      }
      eventually(timeout(Span(2, Minutes))) {
        client.getAllCourses(None, None).handleRequestHeader(addAuthorizationHeader()).invoke().map { answer =>
          answer should contain allOf (course0, course1, course2)
        }
      }
    }

    "get all courses with matching names" in {
      client.getAllCourses(Some("Course 1"), None).handleRequestHeader(addAuthorizationHeader()).invoke().map { answer =>
        answer should contain only (course1, course2)
      }
    }

    "get all courses with matching lecturerIds" in {
      client.getAllCourses(None, Some("11")).handleRequestHeader(addAuthorizationHeader()).invoke().map { answer =>
        answer should contain only (course0, course1)
      }
    }

    "get all courses with matching names and lecturerIds" in {
      client.getAllCourses(Some("Course 1"), Some("11")).handleRequestHeader(addAuthorizationHeader()).invoke().map { answer =>
        answer should contain only course1
      }
    }

    "delete a non-existing course" in {
      client.deleteCourse("42").handleRequestHeader(addAuthorizationHeader()).invoke().failed.map {
        answer =>
          answer.asInstanceOf[CustomException].getErrorCode.http should ===(404)
      }
    }

    "delete an existing course" in {
      client.deleteCourse(course0.courseId).handleRequestHeader(addAuthorizationHeader()).invoke()
        .map(answer => answer shouldBe ===(Done))
      eventually(timeout(Span(2, Minutes))) {
        client.getAllCourses(None, None).handleRequestHeader(addAuthorizationHeader()).invoke().map { answer =>
          answer should not contain course0
        }
      }
    }

    "find a non-existing course" in {
      client.findCourseByCourseId("42").handleRequestHeader(addAuthorizationHeader()).invoke().failed.map { answer =>
        answer.asInstanceOf[CustomException].getErrorCode.http should ===(404)
      }
    }

    "find an existing course" in {
      client.findCourseByCourseId(course1.courseId).handleRequestHeader(addAuthorizationHeader()).invoke().map { answer =>
        answer should ===(course1)
      }
    }

    "update a non-existing course" in {
      client.updateCourse("GutenMorgen").handleRequestHeader(addAuthorizationHeader()).invoke(course3.copy(courseId = "GutenMorgen")).failed.map {
        answer =>
          answer.asInstanceOf[CustomException].getErrorCode.http should ===(404)
      }
    }

    "update an existing course" in {
      val course4 = course2.copy(courseDescription = "CHANGED DESCRIPTION")
      client.updateCourse(course2.courseId).handleRequestHeader(addAuthorizationHeader()).invoke(course4).map { answer =>
        answer should ===(Done)
      }
      eventually(timeout(Span(30, Seconds))) {
        client.findCourseByCourseId(course4.courseId).handleRequestHeader(addAuthorizationHeader()).invoke().map { answer =>
          answer should ===(course4)
        }
      }
    }
  }
}
