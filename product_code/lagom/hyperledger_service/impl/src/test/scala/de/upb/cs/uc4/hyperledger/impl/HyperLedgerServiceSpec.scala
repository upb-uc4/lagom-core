package de.upb.cs.uc4.hyperledger.impl

import akka.Done
import com.lightbend.lagom.scaladsl.api.transport.NotFound
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import de.upb.cs.uc4.hyperledger.api.HyperLedgerService
import de.upb.cs.uc4.hyperledger.traits.{ChaincodeTrait, ConnectionManagerTrait}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

/** Tests for the CourseService
  * All tests need to be started in the defined order
  */
class HyperLedgerServiceSpec extends AsyncWordSpec with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  private val server = ServiceTest.startServer(
    ServiceTest.defaultSetup
      .withCluster()
  ) { ctx =>
    new HyperLedgerApplication(ctx) with LocalServiceLocator {
      override lazy val connectionManager: ConnectionManagerTrait = () => new ChaincodeTrait {

        override def addCourse(jSonCourse: String): String = "???"

        override def getAllCourses(): String = "???"

        override def getCourseById(courseId: String): String = courseId match {
          case "A" => "This is course A"
          case "B" => "This is course B"
          case "C" => "This is course C"
          case _ => throw NotFound("This course does not exist")
        }

        override def deleteCourseById(courseId: String): String = "???"

        override def updateCourseById(courseId: String, jSonCourse: String): String = "???"

        override def close(): Unit = {}
      }
    }
  }

  val client: HyperLedgerService = server.serviceClient.implement[HyperLedgerService]

  override protected def afterAll(): Unit = server.stop()

  /** Tests only working if the whole instance is started */
  "HyperLedgerService service" should {

    "read a course" in {
      client.read("A").invoke().map{ answer =>
        answer should  ===("This is course A")
      }
    }

    "not read a non-existing course" in {
      client.read("E").invoke().failed.map{ answer =>
        answer shouldBe a [NotFound]
      }
    }

    "write a course" in {
      client.write().invoke("Test").map{ answer =>
        answer should ===(Done)
      }
    }
  }
}
