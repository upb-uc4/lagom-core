package de.upb.cs.uc4.impl

import akka.Done
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import de.upb.cs.uc4.api.CourseService
import de.upb.cs.uc4.model.Course
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

class ContentServiceSpec extends AsyncWordSpec with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  private val server = ServiceTest.startServer(
    ServiceTest.defaultSetup
      .withCassandra()
  ) { ctx =>
    new CourseApplication(ctx) with LocalServiceLocator
  }

  val client: CourseService = server.serviceClient.implement[CourseService]

  val course0: Course = Course(18, "Course 0", "Lecture", "Today", "Tomorrow", 8, 11, 60, 20, "german", "A test")
  val course1: Course = Course(17, "Course 1", "Lecture", "Today", "Tomorrow", 8, 11, 60, 20, "german", "A test")
  val course2: Course = Course(16, "Course 2", "Lecture", "Today", "Tomorrow", 8, 12, 60, 20, "german", "A test")
  val course3: Course = Course(18, "Course 3", "Lecture", "Today", "Tomorrow", 8, 11, 60, 20, "german", "A test")

  val sort: (Course, Course) => Boolean = (c1, c2) => c1.courseId < c2.courseId

  override protected def afterAll(): Unit = server.stop()

  "CourseService service" should {

    "get all courses with no courses" in {
      client.getAllCourses.invoke().map { answer =>
        answer should ===(Seq())
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

    "create a course with an id with is already in use" in {
      client.addCourse()
    }
  }
}
