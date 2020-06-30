package de.upb.cs.uc4.course.impl

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport.{NotFound, RequestHeader, TransportException}
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import de.upb.cs.uc4.authentication.api.AuthenticationService
import de.upb.cs.uc4.authentication.model.AuthenticationRole
import de.upb.cs.uc4.authentication.model.AuthenticationRole.AuthenticationRole
import de.upb.cs.uc4.course.api.CourseService
import de.upb.cs.uc4.course.model.{Course, CourseLanguage, CourseType}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

import scala.concurrent.Future

/** Tests for the CourseService
  * All tests need to be started in the defined order
  */
class CourseServiceSpec extends AsyncWordSpec with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  private val server = ServiceTest.startServer(
    ServiceTest.defaultSetup
      .withCassandra()
  ) { ctx =>
    new CourseApplication(ctx) with LocalServiceLocator {
      override lazy val authenticationService: AuthenticationService = new AuthenticationService {

        override def login(): ServiceCall[NotUsed, String] = ServiceCall{ _ => Future.successful("")}

        override def logout(): ServiceCall[NotUsed, Done] = ServiceCall{ _ => Future.successful(Done)}

        override def check(jws: String): ServiceCall[NotUsed, (String, AuthenticationRole)] =
          ServiceCall{ _ => Future.successful("admin", AuthenticationRole.Admin)}

        override def getRole(username: String): ServiceCall[NotUsed, AuthenticationRole] =
          ServiceCall{ _ => Future.successful(AuthenticationRole.Admin)}
      }
    }
  }

  val client: CourseService = server.serviceClient.implement[CourseService]

  //Test courses
  val course0: Course = Course("", "Course 0", CourseType.Lecture, "2020-04-11", "2020-08-01", 8, "11", 60, 20, CourseLanguage.German, "A test")
  val course1: Course = Course("", "Course 1", CourseType.Lecture, "2020-04-11", "2020-08-01", 8, "11", 60, 20, CourseLanguage.German, "A test")
  val course2: Course = Course("", "Course 1", CourseType.Lecture, "2020-04-11", "2020-08-01", 8, "12", 60, 20, CourseLanguage.German, "A test")
  val course3: Course = Course("", "Course 3", CourseType.Lecture, "2020-04-11", "2020-08-01", 8, "11", 60, 20, CourseLanguage.German, "A test")

  override protected def afterAll(): Unit = server.stop()

  def addJwtsHeader(): RequestHeader => RequestHeader = { header =>
    header.withHeader("Authorization", "Bearer MOCK")
  }

  /** Tests only working if the whole instance is started */
  "CourseService service" should {

    "get all courses with no courses" in {
      client.getAllCourses.handleRequestHeader(addJwtsHeader()).invoke().map { answer =>
        answer shouldBe empty
      }
    }

    "create a course" in {
      client.addCourse().handleRequestHeader(addJwtsHeader()).invoke(course0).map { answer =>
        answer should ===(Done)
      }
    }

    "create a second course" in {
      client.addCourse().handleRequestHeader(addJwtsHeader()).invoke(course1).map { answer =>
        answer should ===(Done)
      }
    }

    "create a third course" in {
      client.addCourse().handleRequestHeader(addJwtsHeader()).invoke(course2).map { answer =>
        answer should ===(Done)
      }
    }

    "delete a non-existing course" in {
      client.deleteCourse("42").handleRequestHeader(addJwtsHeader()).invoke().failed.map{
        answer =>
          answer.asInstanceOf[TransportException].errorCode.http should ===(404)
      }
    }

    "find a non-existing course" in {
      client.findCourseByCourseId("42").handleRequestHeader(addJwtsHeader()).invoke().failed.map{ answer =>
          answer shouldBe a [NotFound]
      }
    }

    "update a non-existing course" in {
      client.updateCourse(course3.courseId).handleRequestHeader(addJwtsHeader()).invoke(course3).failed.map{
        answer =>
          answer.asInstanceOf[TransportException].errorCode.http should ===(404)
      }
    }
  }
}
