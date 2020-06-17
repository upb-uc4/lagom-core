package de.upb.cs.uc4.course.impl

import java.util.Base64

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport.{NotFound, RequestHeader, TransportException}
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import de.upb.cs.uc4.authentication.api.AuthenticationService
import de.upb.cs.uc4.authentication.model.AuthenticationResponse
import de.upb.cs.uc4.authentication.model.AuthenticationResponse.AuthenticationResponse
import de.upb.cs.uc4.course.api.CourseService
import de.upb.cs.uc4.course.model.Course
import de.upb.cs.uc4.user.model.Role.Role
import de.upb.cs.uc4.user.model.{JsonRole, Role, User}
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
        /** Checks if the username and password pair exists */
        override def check(username: String, password: String): ServiceCall[Seq[Role], AuthenticationResponse] =
          ServiceCall { _ =>  Future.successful(AuthenticationResponse.Correct)}

        /** Gets the role of a user */
        override def getRole(username: String): ServiceCall[NotUsed, JsonRole] =
          ServiceCall { _ =>  Future.successful(JsonRole(Role.Admin))}

        /** Sets authentication and password of a user */
        override def set(): ServiceCall[User, Done] =
          ServiceCall { _ =>  Future.successful(Done)}

        /** Deletes authentication and password of a user  */
        override def delete(username: String): ServiceCall[NotUsed, Done] =
          ServiceCall { _ =>  Future.successful(Done)}

        /** Allows GET, POST, DELETE, OPTIONS*/
        override def options(): ServiceCall[NotUsed, Done] =
          ServiceCall { _ =>  Future.successful(Done)}

        /** Allows GET, OPTIONS */
        override def optionsGet(): ServiceCall[NotUsed, Done] =
          ServiceCall { _ =>  Future.successful(Done)}

        /** Allows POST */
        override def allowedMethodsPOST(): ServiceCall[NotUsed, Done] = ServiceCall { _ =>  Future.successful(Done)}

        /** Allows GET */
        override def allowedMethodsGET(): ServiceCall[NotUsed, Done] = ServiceCall { _ =>  Future.successful(Done)}

        /** Allows DELETE */
        override def allowedMethodsDELETE(): ServiceCall[NotUsed, Done] = ServiceCall { _ =>  Future.successful(Done)}
      }
    }
  }

  val client: CourseService = server.serviceClient.implement[CourseService]

  //Test courses
  val course0: Course = Course("18", "Course 0", "Lecture", "Today", "Tomorrow", 8, "11", 60, 20, "german", "A test")
  val course1: Course = Course("17", "Course 1", "Lecture", "Today", "Tomorrow", 8, "11", 60, 20, "german", "A test")
  val course2: Course = Course("16", "Course 2", "Lecture", "Today", "Tomorrow", 8, "12", 60, 20, "german", "A test")
  val course3: Course = Course("18", "Course 1", "Lecture", "Today", "Tomorrow", 8, "11", 60, 20, "german", "A test")

  override protected def afterAll(): Unit = server.stop()

  def addAuthorizationHeader(): RequestHeader => RequestHeader = { header =>
    header.withHeader("Authorization", "Basic " + Base64.getEncoder.encodeToString(s"MOCK:MOCK".getBytes()))
  }

  /** Tests only working if the whole instance is started */
  "CourseService service" should {

    "get all courses with no courses" in {
      client.getAllCourses.handleRequestHeader(addAuthorizationHeader()).invoke().map { answer =>
        answer shouldBe empty
      }
    }

    "create a course" in {
      client.addCourse().handleRequestHeader(addAuthorizationHeader()).invoke(course0).map { answer =>
        answer should ===(Done)
      }
    }

    "create a second course" in {
      client.addCourse().handleRequestHeader(addAuthorizationHeader()).invoke(course1).map { answer =>
        answer should ===(Done)
      }
    }

    "create a third course" in {
      client.addCourse().handleRequestHeader(addAuthorizationHeader()).invoke(course2).map { answer =>
        answer should ===(Done)
      }
    }

    "delete a non-existing course" in {
      client.deleteCourse("42").handleRequestHeader(addAuthorizationHeader()).invoke().failed.map{
        answer =>
          answer.asInstanceOf[TransportException].errorCode.http should ===(404)
      }
    }

    "find a non-existing course" in {
      client.findCourseByCourseId("42").handleRequestHeader(addAuthorizationHeader()).invoke().failed.map{ answer =>
          answer shouldBe a [NotFound]
      }
    }

    "update a non-existing course" in {
      client.updateCourse(course2.courseId).handleRequestHeader(addAuthorizationHeader()).invoke(course2).failed.map{
        answer =>
          answer.asInstanceOf[TransportException].errorCode.http should ===(404)
      }
    }
  }
}
