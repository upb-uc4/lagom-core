package de.upb.cs.uc4.hl.course.impl

import java.util.Base64

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport.{BadRequest, RequestHeader, TransportException}
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import de.upb.cs.uc4.authentication.api.AuthenticationService
import de.upb.cs.uc4.authentication.model.AuthenticationRole
import de.upb.cs.uc4.authentication.model.AuthenticationRole.AuthenticationRole
import de.upb.cs.uc4.course.api.CourseService
import de.upb.cs.uc4.course.model.{Course, CourseLanguage, CourseType}
import de.upb.cs.uc4.hl.course.api.HlCourseService
import de.upb.cs.uc4.hyperledger.api.HyperLedgerService
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import play.api.libs.json.Json

import scala.concurrent.Future

/** Tests for the CourseService
  * All tests need to be started in the defined order
  */
class HlCourseServiceSpec extends AsyncWordSpec with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  private val server = ServiceTest.startServer(
    ServiceTest.defaultSetup
  ) { ctx =>
    new HlCourseApplication(ctx) with LocalServiceLocator {
      override lazy val authenticationService: AuthenticationService = new AuthenticationService {

        override def check(username: String, password: String): ServiceCall[NotUsed, (String, AuthenticationRole)] =
          ServiceCall { _ => Future.successful("admin", AuthenticationRole.Admin) }

      }

      override lazy val hyperLedgerService: HyperLedgerService = new HyperLedgerService {
        override def write(transactionId: String): ServiceCall[Seq[String], Done] = ServiceCall { seq =>
          transactionId match {
            case "deleteCourseById" =>
              if (Seq("0", "1").contains(seq.head)) {
                Future.successful(Done)
              } else {
                throw BadRequest("ERROR")
              }
            case "updateCourseById"  =>
              if (Seq("0", "1").contains(seq.head)) {
                Future.successful(Done)
              } else {
                throw BadRequest("ERROR")
              }
            case _ => Future.successful(Done)
          }
        }

        override def read(transactionId: String): ServiceCall[Seq[String], String] = ServiceCall { seq =>
          transactionId match {
            case "getAllCourses" => Future.successful("[]")
            case "getCourseById" => seq.head match {
              case course0.courseId => Future.successful(Json.stringify(Json.toJson(course0)))
              case course2.courseId => Future.successful(Json.stringify(Json.toJson(course2)))
              case course3.courseId => Future.successful(Json.stringify(Json.toJson(course0)))
              case _ => throw BadRequest("ERROR")
            }
            case _ => Future.successful("")
          }
        }
      }
    }
  }

  val client: CourseService = server.serviceClient.implement[HlCourseService]

  //Test courses
  val course0: Course = Course("0", "Course 0", CourseType.Lecture.toString, "2020-04-11", "2020-08-01", 8, "11", 60, 20, CourseLanguage.German.toString, "A test")
  val course1: Course = Course("1", "Course 1", CourseType.Lecture.toString, "2020-04-11", "2020-08-01", 8, "11", 60, 20, CourseLanguage.German.toString, "A test")
  val course2: Course = Course("1", "Course 1", CourseType.Lecture.toString, "2020-04-11", "2020-08-01", 8, "12", 60, 20, CourseLanguage.German.toString, "A test")
  val course3: Course = Course("2", "Course 3", CourseType.Lecture.toString, "2020-04-11", "2020-08-01", 8, "11", 60, 20, CourseLanguage.German.toString, "A test")

  override protected def afterAll(): Unit = server.stop()

  def addAuthenticationHeader(): RequestHeader => RequestHeader = { header =>
    header.withHeader("Authorization", "Basic " + Base64.getEncoder.encodeToString("MOCK:MOCK".getBytes()))
  }

  /** Tests only working if the whole instance is started */
  "CourseService service" should {

    "get all courses with no courses" in {
      client.getAllCourses(None, None).handleRequestHeader(addAuthenticationHeader()).invoke().map { answer =>
        answer shouldBe empty
      }
    }

    "create a course" in {
      client.addCourse().handleRequestHeader(addAuthenticationHeader()).invoke(course0).map { answer =>
        answer.courseName should ===(course0.courseName)
        answer.courseDescription should ===(course0.courseDescription)
        answer.courseLanguage should ===(course0.courseLanguage)
        answer.courseType should ===(course0.courseType)
        answer.currentParticipants should ===(course0.currentParticipants)
      }
    }

    "create a second course" in {
      client.addCourse().handleRequestHeader(addAuthenticationHeader()).invoke(course1).map { answer =>
        answer.courseName should ===(course1.courseName)
        answer.courseDescription should ===(course1.courseDescription)
        answer.courseLanguage should ===(course1.courseLanguage)
        answer.courseType should ===(course1.courseType)
        answer.currentParticipants should ===(course1.currentParticipants)
      }
    }

    "create a third course" in {
      client.addCourse().handleRequestHeader(addAuthenticationHeader()).invoke(course2).map { answer =>
        answer.courseName should ===(course2.courseName)
        answer.courseDescription should ===(course2.courseDescription)
        answer.courseLanguage should ===(course2.courseLanguage)
        answer.courseType should ===(course2.courseType)
        answer.currentParticipants should ===(course2.currentParticipants)
      }
    }

    "delete a non-existing course" in {
      client.deleteCourse("42").handleRequestHeader(addAuthenticationHeader()).invoke().failed.map {
        answer =>
          answer.asInstanceOf[TransportException].errorCode.http should ===(400)
      }
    }

    "find a non-existing course" in {
      client.findCourseByCourseId("42").handleRequestHeader(addAuthenticationHeader()).invoke().failed.map { answer =>
        answer.asInstanceOf[TransportException].errorCode.http should ===(400)
      }
    }

    "update a non-existing course" in {
      client.updateCourse(course3.courseId).handleRequestHeader(addAuthenticationHeader()).invoke(course3).failed.map {
        answer =>
          answer.asInstanceOf[TransportException].errorCode.http should ===(400)
      }
    }
  }
}
