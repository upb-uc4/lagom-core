package de.upb.cs.uc4.hyperledger.hyperledger_service.test

import java.nio.file.Paths

import akka.Done
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatest.matchers.should.Matchers
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import de.upb.cs.uc4.course.model.Course
import de.upb.cs.uc4.hyperledger.ConnectionManager
import de.upb.cs.uc4.hyperledger.traits._
import de.upb.cs.uc4.hyperledger.impl._
import de.upb.cs.uc4.hyperledger.api._
import de.upb.cs.uc4.shared.client.CustomException
import de.upb.cs.uc4.test_resources._
import play.api.libs.json.Json

/** Tests for the CourseService
 * All tests need to be started in the defined order
 */
class HyperledgerServiceTest extends AsyncWordSpec with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  private lazy val server = ServiceTest.startServer(ServiceTest.defaultSetup.withCluster()) { ctx =>
    new HyperLedgerApplication(ctx) with LocalServiceLocator {
      override lazy val connectionManager: ConnectionManagerTrait = ConnectionManager(
        Paths.get(getClass.getResource("/connection_profile.yaml").toURI),
        Paths.get(getClass.getResource("/wallet/").toURI)
      )
    }
  }

  val client: HyperLedgerService = server.serviceClient.implement[HyperLedgerService]

  override protected def afterAll(): Unit = server.stop()

  /** Tests only working if the whole instance is started */
  "HyperLedgerService service" should {

    "read empty list of all courses" in {
      client.read("getAllCourses").invoke(List()).map { answer => {
        answer should ===("[]")
      }}
    }

    "not read a non-existing course" in {
      client.read("getCourseById").invoke(List("invalidID")).failed.map { answer =>
        answer.asInstanceOf[CustomException].getErrorCode.http should ===(404)
      }
    }

    "write a course" in {
      client.write("addCourse").invoke(List(TestCourses.courseA)).map { answer =>
        answer should ===(Done)
      }
    }

    "not write a ill-formatted course" in {
      client.write("addCourse").invoke(List(TestCourses.courseInvalid)).failed.map { answer =>
        answer shouldBe a [Exception]
      }
    }

    "read a course" in {
      client.read("getCourseById").invoke(List("A")).map { answer =>
        Json.parse(answer).as[Course] should ===(Json.parse(TestCourses.courseA).as[Course])
      }
    }

    "not write a non json" in {
      client.write("addCourse").invoke(List("invalid")).failed.map { answer =>
        answer.asInstanceOf[CustomException].getErrorCode.http should ===(500)
      }
    }

    "delete a course" in {
      client.write("deleteCourseById").invoke(List("A")).map { answer =>
        answer should ===(Done)
      }
    }

    "not delete a non-existing course" in {
      client.write("deleteCourseById").invoke(List("invalidID")).failed.map { answer =>
        answer.asInstanceOf[CustomException].getErrorCode.http should ===(500)
      }
    }
  }
}