package de.upb.cs.uc4.course.impl

import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import de.upb.cs.uc4.course.DefaultTestCourses
import de.upb.cs.uc4.course.api.CourseService
import de.upb.cs.uc4.course.model.Course
import de.upb.cs.uc4.examreg.api.ExamregService
import de.upb.cs.uc4.examreg.{ DefaultTestExamRegs, ExamregServiceStub }
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
  with UC4SpecUtils with Matchers with BeforeAndAfterAll with Eventually with DefaultTestCourses with DefaultTestExamRegs {

  private val server = ServiceTest.startServer(
    ServiceTest.defaultSetup
      .withJdbc()
  ) { ctx =>
      new CourseApplication(ctx) with LocalServiceLocator {
        override lazy val userService: UserServiceStub = new UserServiceStub()
        userService.resetToDefaults()
        override lazy val examregService: ExamregService = new ExamregServiceStub()
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
    client.getAllCourses(None, None, None).handleRequestHeader(addAuthorizationHeader()).invoke().flatMap {
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

  val course1WithModules: Course = course1.copy(moduleIds = Seq(examReg0.modules.head.id))
  val course2WithModules: Course = course2.copy(moduleIds = examReg0.modules.map(_.id))

  /** Tests only working if the whole instance is started */
  "CourseService" should {

    "get all courses without authorization" in {
      client.getAllCourses(None, None, None)
        .invoke().map { answer =>
          answer shouldBe empty
        }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "get all courses with matching names" in {
      prepare(Seq(course0, course1, course2)).flatMap { _ =>
        client.getAllCourses(Some("Course 1"), None, None).handleRequestHeader(addAuthorizationHeader())
          .invoke().map { answer =>
            answer.map(_.copy(courseId = "")) should contain theSameElementsAs Seq(course1, course2)
          }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "get all courses with matching lecturerIds" in {
      prepare(Seq(course0, course1, course2)).flatMap { _ =>
        client.getAllCourses(None, Some("lecturer0"), None).handleRequestHeader(addAuthorizationHeader())
          .invoke().map { answer =>
            answer.map(_.copy(courseId = "")) should contain theSameElementsAs Seq(course0, course1)
          }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "get all courses with matching moduleId" in {
      prepare(Seq(course0, course1WithModules, course2WithModules)).flatMap { _ =>
        client.getAllCourses(None, None, Some(examReg0.modules.head.id)).handleRequestHeader(addAuthorizationHeader())
          .invoke().map { answer =>
            answer.map(_.copy(courseId = "")) should contain theSameElementsAs Seq(course1WithModules, course2WithModules)
          }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "get all courses with matching examregNames" in {
      prepare(Seq(course0, course1WithModules, course2WithModules)).flatMap { _ =>
        client.getAllCourses(None, None, None, Some(examReg0.name)).handleRequestHeader(addAuthorizationHeader())
          .invoke().map { answer =>
            answer.map(_.copy(courseId = "")) should contain theSameElementsAs Seq(course1WithModules, course2WithModules)
          }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "get all courses with matching names and lecturerIds and moduleId and examregNames" in {
      prepare(Seq(course0, course1WithModules, course2)).flatMap { _ =>
        client.getAllCourses(Some(course1WithModules.courseName), Some(course1WithModules.lecturerId), Some(course1WithModules.moduleIds.head), Some(examReg0.name)).handleRequestHeader(addAuthorizationHeader())
          .invoke().map { answer =>
            answer.map(_.copy(courseId = "")) should contain theSameElementsAs Seq(course1WithModules)
          }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "fail in finding a non-existing course" in {
      client.findCourseByCourseId("42")
        .invoke().failed.map { answer =>
          answer.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.KeyNotFound)
        }
    }

    "find an existing course" in {
      prepare(Seq(course1)).flatMap { createdCourses =>
        client.findCourseByCourseId(createdCourses.head.courseId)
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
            client.getAllCourses(None, None, None).handleRequestHeader(addAuthorizationHeader())
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
            client.getAllCourses(None, None, None).handleRequestHeader(addAuthorizationHeader())
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

    "not create a course as an Admin with a non existing module" in {
      client.addCourse().handleRequestHeader(addAuthorizationHeader())
        .invoke(course0.copy(moduleIds = Seq("nonExisting", examReg0.modules.head.id, "nonExisting2"))).failed.map {
          answer =>
            answer.asInstanceOf[UC4Exception].possibleErrorResponse.asInstanceOf[DetailedError].invalidParams.map(_.name) should
              contain theSameElementsAs Seq("moduleIds[0]", "moduleIds[2]")
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

    "not create a course with an malformed lecturerId" in {
      client.addCourse().handleRequestHeader(addAuthorizationHeader())
        .invoke(course0.copy(lecturerId = "")).failed.map { answer =>
          answer.asInstanceOf[UC4Exception].possibleErrorResponse.asInstanceOf[DetailedError].invalidParams.map(_.name) should contain theSameElementsAs Seq("lecturerId")
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
              client.getAllCourses(None, None, None).handleRequestHeader(addAuthorizationHeader())
                .invoke().map { answer =>
                  answer should not contain createdCourses.head
                }
            }
          }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "not delete an existing course from another lecturer" in {
      prepare(Seq(course2)).flatMap { createdCourses =>
        client.deleteCourse(createdCourses.head.courseId).handleRequestHeader(addAuthorizationHeader("lecturer0"))
          .invoke().failed.flatMap {
            answer => answer.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.OwnerMismatch)
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

    "not update a course of with missmatching id, as a lecturer" in {
      prepare(Seq(course0, course1)).flatMap { createdCourses =>
        client.updateCourse(createdCourses.head.courseId).handleRequestHeader(addAuthorizationHeader("lecturer0"))
          .invoke(createdCourses.tail.head.copy(courseId = "newId")).failed.map {
            answer =>
              answer.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.PathParameterMismatch)
          }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
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
              client.findCourseByCourseId(course4.courseId)
                .invoke().map { answer =>
                  answer should ===(course4)
                }
            }
          }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "not update an existing course with a new course containing a malformed lecturerId" in {
      prepare(Seq(course2)).flatMap { createdCourses =>
        val course4 = createdCourses.head.copy(lecturerId = "12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901")
        client.updateCourse(createdCourses.head.courseId).handleRequestHeader(addAuthorizationHeader())
          .invoke(course4).failed.flatMap { answer =>
            answer.asInstanceOf[UC4Exception].possibleErrorResponse.asInstanceOf[DetailedError].invalidParams.map(_.name) should
              contain theSameElementsAs Seq("lecturerId")
          }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

    "not update an existing course with a non existing moduleId" in {
      prepare(Seq(course2)).flatMap { createdCourses =>
        val course4 = createdCourses.head.copy(moduleIds = Seq("hallo"))
        client.updateCourse(createdCourses.head.courseId).handleRequestHeader(addAuthorizationHeader())
          .invoke(course4).failed.flatMap {
            answer =>
              answer.asInstanceOf[UC4Exception].possibleErrorResponse.asInstanceOf[DetailedError].invalidParams.map(_.name) should
                contain theSameElementsAs Seq("moduleIds[0]")
          }
      }.flatMap(cleanupOnSuccess)
        .recoverWith(cleanupOnFailure())
    }

  }
}
