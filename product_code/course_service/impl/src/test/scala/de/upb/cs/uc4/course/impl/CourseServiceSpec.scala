package de.upb.cs.uc4.course.impl

import java.util.Calendar

import com.lightbend.lagom.scaladsl.api.transport.RequestHeader
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import de.upb.cs.uc4.authentication.model.AuthenticationRole
import de.upb.cs.uc4.authentication.model.AuthenticationRole.AuthenticationRole
import de.upb.cs.uc4.course.api.CourseService
import de.upb.cs.uc4.course.model.{ Course, CourseLanguage, CourseType }
import de.upb.cs.uc4.shared.client.exceptions.CustomException
import io.jsonwebtoken.{ Jwts, SignatureAlgorithm }
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{ Seconds, Span }
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatest.{ Assertion, BeforeAndAfterAll }

import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }

/** Tests for the CourseService
  * All tests need to be started in the defined order
  */
class CourseServiceSpec extends AsyncWordSpec with Matchers with BeforeAndAfterAll with Eventually {

  private val server = ServiceTest.startServer(
    ServiceTest.defaultSetup
      .withJdbc()
  ) { ctx =>
      new CourseApplication(ctx) with LocalServiceLocator
    }

  val client: CourseService = server.serviceClient.implement[CourseService]

  //Test courses
  var course0: Course = Course("", "Course 0", CourseType.Lecture.toString, "2020-04-11", "2020-08-01", 8, "11", 60, 20, CourseLanguage.German.toString, "A test")
  var course1: Course = Course("", "Course 1", CourseType.Lecture.toString, "2020-04-11", "2020-08-01", 8, "11", 60, 20, CourseLanguage.German.toString, "A test")
  var course2: Course = Course("", "Course 1", CourseType.Lecture.toString, "2020-04-11", "2020-08-01", 8, "12", 60, 20, CourseLanguage.German.toString, "A test")
  var course3: Course = Course("", "Course 3", CourseType.Lecture.toString, "2020-04-11", "2020-08-01", 8, "11", 60, 20, CourseLanguage.German.toString, "A test")

  override protected def afterAll(): Unit = server.stop()

  def addAuthorizationHeader(username: String = "username", role: AuthenticationRole = AuthenticationRole.Admin): RequestHeader => RequestHeader = { header =>
    val time = Calendar.getInstance()
    time.add(Calendar.DATE, 1)

    val token =
      Jwts.builder()
        .setSubject("login")
        .setExpiration(time.getTime)
        .claim("username", username)
        .claim("authenticationRole", role.toString)
        .signWith(SignatureAlgorithm.HS256, "changeme")
        .compact()

    header.withHeader("Cookie", s"login=$token")
  }

  def prepare(courses: Seq[Course]): Future[Seq[Course]] = {
    Future.sequence(courses.map { course =>
      client.addCourse().handleRequestHeader(addAuthorizationHeader()).invoke(course)
    }).flatMap { createdCourses =>
      eventually(timeout(Span(15, Seconds))) {
        for {
          courseIdsDatabase <- server.application.database.getAll
        } yield {
          courseIdsDatabase should contain theSameElementsAs createdCourses.map(_.courseId)
        }
      }.map(_ => createdCourses)
    }
  }

  def deleteAllCourses(): Future[Assertion] = {
    client.getAllCourses(None, None).handleRequestHeader(addAuthorizationHeader()).invoke().flatMap {
      list =>
        Future.sequence(
          list.map { course =>
            client.deleteCourse(course.courseId).handleRequestHeader(addAuthorizationHeader()).invoke()
          }
        )
    }.flatMap { _ =>
      eventually(timeout(Span(15, Seconds))) {
        for {
          courseNames <- server.application.database.getAll
        } yield {
          courseNames shouldBe empty
        }
      }
    }
  }

  def cleanupOnFailure(): PartialFunction[Throwable, Future[Assertion]] = PartialFunction.fromFunction { x =>
    deleteAllCourses()
      .map { _ =>
        throw x
      }
  }
  def cleanupOnSuccess(value: Assertion): Future[Assertion] = {
    deleteAllCourses()
      .map { _ =>
        value
      }
  }

  /** Tests only working if the whole instance is started */
  "CourseService" should {

    "get all courses with no courses" in {
      client.getAllCourses(None, None).handleRequestHeader(addAuthorizationHeader()).invoke().map { answer =>
        answer shouldBe empty
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "create a course" in {
      client.addCourse().handleRequestHeader(addAuthorizationHeader()).invoke(course0).flatMap { createdCourse =>

        eventually(timeout(Span(15, Seconds))) {
          client.getAllCourses(None, None).handleRequestHeader(addAuthorizationHeader()).invoke().map { answer =>
            answer should contain theSameElementsAs Seq(createdCourse)
          }
        }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "get all courses with matching names" in {
      prepare(Seq(course0, course1, course2)).flatMap { _ =>
        client.getAllCourses(Some("Course 1"), None).handleRequestHeader(addAuthorizationHeader()).invoke().map { answer =>
          answer.map(_.copy(courseId = "")) should contain theSameElementsAs Seq(course1, course2)
        }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "get all courses with matching lecturerIds" in {
      prepare(Seq(course0, course1, course2)).flatMap { _ =>
        client.getAllCourses(None, Some("11")).handleRequestHeader(addAuthorizationHeader()).invoke().map { answer =>
          answer.map(_.copy(courseId = "")) should contain theSameElementsAs Seq(course0, course1)
        }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "get all courses with matching names and lecturerIds" in {
      prepare(Seq(course0, course1, course2)).flatMap { _ =>
        client.getAllCourses(Some("Course 1"), Some("11")).handleRequestHeader(addAuthorizationHeader()).invoke().map { answer =>
          answer.map(_.copy(courseId = "")) should contain theSameElementsAs Seq(course1)
        }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "delete a non-existing course" in {
      client.deleteCourse("42").handleRequestHeader(addAuthorizationHeader()).invoke().failed.map {
        answer =>
          answer.asInstanceOf[CustomException].getErrorCode.http should ===(404)
      }
    }

    "delete an existing course" in {
      prepare(Seq(course2)).flatMap { createdCourses =>
        client.deleteCourse(createdCourses.head.courseId).handleRequestHeader(addAuthorizationHeader()).invoke().flatMap { _ =>

          eventually(timeout(Span(15, Seconds))) {
            client.getAllCourses(None, None).handleRequestHeader(addAuthorizationHeader()).invoke().map { answer =>
              answer should not contain createdCourses.head
            }
          }
        }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "find a non-existing course" in {
      client.findCourseByCourseId("42").handleRequestHeader(addAuthorizationHeader()).invoke().failed.map { answer =>
        answer.asInstanceOf[CustomException].getErrorCode.http should ===(404)
      }
    }

    "find an existing course" in {
      prepare(Seq(course1)).flatMap { createdCourses =>
        client.findCourseByCourseId(createdCourses.head.courseId).handleRequestHeader(addAuthorizationHeader()).invoke().map { answer =>
          answer should ===(createdCourses.head)
        }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "update a non-existing course" in {
      client.updateCourse("GutenMorgen").handleRequestHeader(addAuthorizationHeader()).invoke(course3.copy(courseId = "GutenMorgen")).failed.map {
        answer =>
          answer.asInstanceOf[CustomException].getErrorCode.http should ===(404)
      }
    }

    "update an existing course" in {
      prepare(Seq(course2)).flatMap { createdCourses =>
        val course4 = createdCourses.head.copy(courseDescription = "CHANGED DESCRIPTION")
        client.updateCourse(createdCourses.head.courseId).handleRequestHeader(addAuthorizationHeader()).invoke(course4).flatMap { _ =>
          eventually(timeout(Span(30, Seconds))) {
            client.findCourseByCourseId(course4.courseId).handleRequestHeader(addAuthorizationHeader()).invoke().map { answer =>
              answer should ===(course4)
            }
          }
        }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

  }
}
