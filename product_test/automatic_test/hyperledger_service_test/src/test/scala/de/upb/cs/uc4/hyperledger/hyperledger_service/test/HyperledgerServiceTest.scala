package de.upb.cs.uc4.hyperledger.hyperledger_service.test

import akka.Done
import com.lightbend.lagom.scaladsl.api.transport.{BadRequest, NotFound}
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.wordspec.{AnyWordSpec, AsyncWordSpec}
import org.scalatest.matchers.should.Matchers
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import de.upb.cs.uc4.hyperledger.ConnectionManager
import de.upb.cs.uc4.hyperledger.traits._
import de.upb.cs.uc4.hyperledger.impl._
import de.upb.cs.uc4.hyperledger.api._
import de.upb.cs.uc4.test_resources._

/** Tests for the CourseService
 * All tests need to be started in the defined order
 */
class HyperledgerServiceTest extends AsyncWordSpec with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  private val server = ServiceTest.startServer(ServiceTest.defaultSetup.withCluster()) { ctx =>
    new HyperLedgerApplication(ctx) with LocalServiceLocator {
      override lazy val connectionManager: ConnectionManagerTrait = ConnectionManager()
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
        answer shouldBe a [NotFound]
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
        answer should ===(TestCourses.courseA)
      }
    }

    "not write a non json" in {
      client.write("addCourse").invoke(List("invalid")).failed.map { answer =>
        answer shouldBe a [BadRequest]
      }
    }

    "delete a course" in {
      client.write("deleteCourseById").invoke(List("A")).map { answer =>
        answer should ===(Done)
      }
    }

    "not delete a non-existing course" in {
      client.write("deleteCourseById").invoke(List("invalidID")).failed.map { answer =>
        answer shouldBe a [BadRequest]
      }
    }
  }
}