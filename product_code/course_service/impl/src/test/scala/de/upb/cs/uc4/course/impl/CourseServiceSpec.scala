package de.upb.cs.uc4.course.impl

import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import de.upb.cs.uc4.course.DefaultTestCourses
import de.upb.cs.uc4.course.api.CourseService
import de.upb.cs.uc4.course.model.Course
import de.upb.cs.uc4.shared.client.exceptions.{ DetailedError, ErrorType, UC4Exception }
import de.upb.cs.uc4.shared.server.UC4SpecUtils
import de.upb.cs.uc4.user.UserServiceStub
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{ Seconds, Span }
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatest.{ Assertion, BeforeAndAfterAll }

import scala.concurrent.Future

/** Tests for the CourseService */
class CourseServiceSpec extends AsyncWordSpec
  with UC4SpecUtils with Matchers with BeforeAndAfterAll with Eventually with DefaultTestCourses {

  private val server = ServiceTest.startServer(
    ServiceTest.defaultSetup
      .withJdbc()
  ) { ctx =>
      new CourseApplication(ctx) with LocalServiceLocator {
        override lazy val userService: UserServiceStub = new UserServiceStub()
        userService.resetToDefaults()
      }
    }

  val client: CourseService = server.serviceClient.implement[CourseService]

  override protected def afterAll(): Unit = server.stop()

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

  def cleanupOnFailure(): PartialFunction[Throwable, Future[Assertion]] = PartialFunction.fromFunction { throwable =>
    deleteAllCourses()
      .map { _ =>
        throw throwable
      }
  }
  def cleanupOnSuccess(assertion: Assertion): Future[Assertion] = {
    deleteAllCourses()
      .map { _ =>
        assertion
      }
  }

  /** Tests only working if the whole instance is started */
  "CourseService" should {

    "get all courses with no courses" in {
      client.getAllCourses(None, None).handleRequestHeader(addAuthorizationHeader())
        .invoke().map { answer =>
          answer shouldBe empty
        }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "get all courses with matching names" in {
      prepare(Seq(course0, course1, course2)).flatMap { _ =>
        client.getAllCourses(Some("Course 1"), None).handleRequestHeader(addAuthorizationHeader())
          .invoke().map { answer =>
            answer.map(_.copy(courseId = "")) should contain theSameElementsAs Seq(course1, course2)
          }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "get all courses with matching lecturerIds" in {
      prepare(Seq(course0, course1, course2)).flatMap { _ =>
        client.getAllCourses(None, Some("lecturer0")).handleRequestHeader(addAuthorizationHeader())
          .invoke().map { answer =>
            answer.map(_.copy(courseId = "")) should contain theSameElementsAs Seq(course0, course1)
          }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "get all courses with matching names and lecturerIds" in {
      prepare(Seq(course0, course1, course2)).flatMap { _ =>
        client.getAllCourses(Some("Course 1"), Some("lecturer0")).handleRequestHeader(addAuthorizationHeader())
          .invoke().map { answer =>
            answer.map(_.copy(courseId = "")) should contain theSameElementsAs Seq(course1)
          }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "fail in finding a non-existing course" in {
      client.findCourseByCourseId("42").handleRequestHeader(addAuthorizationHeader())
        .invoke().failed.map { answer =>
          answer.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.KeyNotFound)
        }
    }

    "find an existing course" in {
      prepare(Seq(course1)).flatMap { createdCourses =>
        client.findCourseByCourseId(createdCourses.head.courseId).handleRequestHeader(addAuthorizationHeader())
          .invoke().map { answer =>
            answer should ===(createdCourses.head)
          }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "create a course as an Admin" in {
      client.addCourse().handleRequestHeader(addAuthorizationHeader())
        .invoke(course0).flatMap { createdCourse =>

          eventually(timeout(Span(15, Seconds))) {
            client.getAllCourses(None, None).handleRequestHeader(addAuthorizationHeader())
              .invoke().map { answer =>
                answer should contain theSameElementsAs Seq(createdCourse)
              }
          }
        }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "create a course as a lecturer" in {
      client.addCourse().handleRequestHeader(addAuthorizationHeader("lecturer0"))
        .invoke(course0.copy(lecturerId = "lecturer0")).flatMap { createdCourse =>

          eventually(timeout(Span(15, Seconds))) {
            client.getAllCourses(None, None).handleRequestHeader(addAuthorizationHeader())
              .invoke().map { answer =>
                answer should contain theSameElementsAs Seq(createdCourse)
              }
          }
        }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "not create a course as an Admin with a non existing lecturer" in {
      client.addCourse().handleRequestHeader(addAuthorizationHeader())
        .invoke(course0.copy(lecturerId = "nonExisting")).failed.map {
          answer =>
            answer.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.Validation)
        }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "not create a course for another lecturer, as a lecturer" in {
      client.addCourse().handleRequestHeader(addAuthorizationHeader("lecturer0"))
        .invoke(course0.copy(lecturerId = "lecturer1")).failed.map { answer =>
          answer.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.OwnerMismatch)
        }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "not create an invalid course" in {
      client.addCourse().handleRequestHeader(addAuthorizationHeader()).invoke(course0.copy(startDate = "ab15-37-42")).failed.map {
        answer =>
          answer.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.Validation)
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "delete a non-existing course" in {
      client.deleteCourse("42").handleRequestHeader(addAuthorizationHeader())
        .invoke().failed.map {
          answer =>
            answer.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.KeyNotFound)
        }
    }

    "delete an existing course" in {
      prepare(Seq(course2)).flatMap { createdCourses =>
        client.deleteCourse(createdCourses.head.courseId).handleRequestHeader(addAuthorizationHeader())
          .invoke().flatMap { _ =>

            eventually(timeout(Span(15, Seconds))) {
              client.getAllCourses(None, None).handleRequestHeader(addAuthorizationHeader())
                .invoke().map { answer =>
                  answer should not contain createdCourses.head
                }
            }
          }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "not update a non-existing course" in {
      client.updateCourse("GutenMorgen").handleRequestHeader(addAuthorizationHeader()).invoke(course3.copy(courseId = "GutenMorgen")).failed.map {
        answer =>
          answer.asInstanceOf[UC4Exception].possibleErrorResponse.asInstanceOf[DetailedError]
            .invalidParams.map(_.name) should contain("courseId")
      }
    }

    "not update a course of another lecturer, as a lecturer" in {
      prepare(Seq(course0)).flatMap { createdCourses =>
        client.updateCourse(createdCourses.head.courseId).handleRequestHeader(addAuthorizationHeader("lecturer1"))
          .invoke(createdCourses.head.copy(startDate = "1996-05-21")).failed.map {
            answer =>
              answer.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.OwnerMismatch)
          }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "not update a course with a non-existing lecturer" in {
      prepare(Seq(course0)).flatMap { createdCourses =>
        client.updateCourse(createdCourses.head.courseId).handleRequestHeader(addAuthorizationHeader())
          .invoke(createdCourses.head.copy(lecturerId = "nonExisting")).failed.map {
            answer =>
              answer.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.Validation)
          }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "not update with an invalid course" in {
      prepare(Seq(course0)).flatMap { createdCourses =>
        client.updateCourse(createdCourses.head.courseId).handleRequestHeader(addAuthorizationHeader())
          .invoke(createdCourses.head.copy(endDate = "15a68d42")).failed.map {
            answer =>
              answer.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.Validation)
          }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "update an existing course" in {
      prepare(Seq(course2)).flatMap { createdCourses =>
        val course4 = createdCourses.head.copy(courseDescription = "CHANGED DESCRIPTION")
        client.updateCourse(createdCourses.head.courseId).handleRequestHeader(addAuthorizationHeader())
          .invoke(course4).flatMap { _ =>
            eventually(timeout(Span(30, Seconds))) {
              client.findCourseByCourseId(course4.courseId).handleRequestHeader(addAuthorizationHeader())
                .invoke().map { answer =>
                  answer should ===(course4)
                }
            }
          }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

  }
}
