package de.upb.cs.uc4.impl

import akka.Done
import com.lightbend.lagom.scaladsl.api.transport.{NotFound, TransportException}
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import de.upb.cs.uc4.api.CourseService
import de.upb.cs.uc4.model.Course
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

/** Tests for the CourseService
  * All tests need to be started in the defined order
  */
class CourseServiceSpec extends AsyncWordSpec with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  private val server = ServiceTest.startServer(
    ServiceTest.defaultSetup
      .withCassandra()
  ) { ctx =>
    new CourseApplication(ctx) with LocalServiceLocator
  }

  val client: CourseService = server.serviceClient.implement[CourseService]

  //Test courses
  val course0: Course = Course(18, "Course 0", "Lecture", "Today", "Tomorrow", 8, 11, 60, 20, "german", "A test")
  val course1: Course = Course(17, "Course 1", "Lecture", "Today", "Tomorrow", 8, 11, 60, 20, "german", "A test")
  val course2: Course = Course(16, "Course 1", "Lecture", "Today", "Tomorrow", 8, 12, 60, 20, "german", "A test")
  val course3: Course = Course(18, "Course 3", "Lecture", "Today", "Tomorrow", 8, 11, 60, 20, "german", "A test")

  override protected def afterAll(): Unit = server.stop()

  /** Tests only working if the whole instance is started */
  "CourseService service" should {

    "get all courses with no courses" in {
      client.getAllCourses.invoke().map { answer =>
        answer shouldBe empty
      }
    }

    "create a course" in {
      client.addCourse().invoke(course0).map { answer =>
        answer should ===(Done)
      }
    }

    "create a second course" in {
      client.addCourse().invoke(course1).map { answer =>
        answer should ===(Done)
      }
    }

    "create a third course" in {
      client.addCourse().invoke(course2).map { answer =>
        answer should ===(Done)
      }
    }

    "create a course with an id with is already in use" in {
      client.addCourse().invoke(course3).failed.map{
        answer =>
          answer.asInstanceOf[TransportException].errorCode.http should ===(409)
      }
    }

    "delete a course" in {
      for {
        _ <- client.deleteCourse().invoke(course2.courseId)
        answer <- client.findCourseByCourseId().invoke(course2.courseId).failed
      }yield{
        answer shouldBe a [NotFound]
      }
    }

    "delete a non-existing course" in {
      client.deleteCourse().invoke(42).failed.map{
        answer =>
          answer.asInstanceOf[TransportException].errorCode.http should ===(404)
      }
    }

    "find a course" in {
      client.findCourseByCourseId().invoke(17).map{answer =>
        answer should ===(course1)
      }
    }

    "find a non-existing course" in {
      client.findCourseByCourseId().invoke(42).failed.map{
        answer =>
          answer shouldBe a [NotFound]
      }
    }

    "update an existing course" in {
      for{
        _ <-client.updateCourse().invoke(course3)
        answer <- client.findCourseByCourseId().invoke(course0.courseId)
      }yield{
        answer should ===(course3)
      }
    }

    "update a non-existing course" in {
      client.updateCourse().invoke(course2).failed.map{
        answer =>
          answer.asInstanceOf[TransportException].errorCode.http should ===(404)
      }
    }
  }
}
