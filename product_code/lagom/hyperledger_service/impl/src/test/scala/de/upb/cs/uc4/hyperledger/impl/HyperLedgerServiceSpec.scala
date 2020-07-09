package de.upb.cs.uc4.hyperledger.impl

import akka.Done
import com.lightbend.lagom.scaladsl.api.transport.{BadRequest, NotFound}
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import de.upb.cs.uc4.hyperledger.api.HyperLedgerService
import de.upb.cs.uc4.hyperledger.traits.{ChaincodeTrait, ConnectionManagerTrait}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import play.api.libs.json.Json

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

        override def close(): Unit = {}

        override def submitTransaction(transactionId: String, params: String*): String = transactionId match {
          case "addCourse" =>
            params.head match {
              case `courseA` => ""
              case `courseB` => ""
              case `courseInvalid` => throw new Exception("""{"name":"50","detail":"ects must be a positive integer number"}""")
            }
          case "deleteCourseById" =>
            params.head match {
              case "validID" => ""
              case "invalidID" => "null"
            }
          case "updateCourseById" =>
            params.head match {
              case "validID" => ""
              case "invalidID" => throw new Exception("""{"name":"03","detail":"Course not found"}""")
            }
          case _ => "Undefined"
        }

        override def evaluateTransaction(transactionId: String, params: String*): String = transactionId match {
          case "getAllCourses" => "[]" //empty
            //not empty = [courseA]
          case "getCourseById" =>
            params.head match {
              case "A" => courseA
              case "B" => courseB
              case "invalidID" => "null"
            }
          case _ => "Undefined"
        }
      }
    }
  }

  val client: HyperLedgerService = server.serviceClient.implement[HyperLedgerService]

  override protected def afterAll(): Unit = server.stop()

  // Examples
  val courseA: String =
    """{
      |  "courseId": "A",
      |  "courseName": "A course",
      |  "courseType": "Lecture",
      |  "startDate": "2020-06-30",
      |  "endDate": "2020-06-30",
      |  "ects": 10,
      |  "lecturerId": "string",
      |  "maxParticipants": 10,
      |  "currentParticipants": 0,
      |  "courseLanguage": "German",
      |  "courseDescription": "string"
      |}""".stripMargin

  val courseB: String =
    """{
      |  "courseId": "B",
      |  "courseName": "B course",
      |  "courseType": "Lecture",
      |  "startDate": "2020-06-30",
      |  "endDate": "2020-06-30",
      |  "ects": 20,
      |  "lecturerId": "string",
      |  "maxParticipants": 120,
      |  "currentParticipants": 0,
      |  "courseLanguage": "German",
      |  "courseDescription": "string"
      |}""".stripMargin

  val courseInvalid: String =
    """{
      |  "courseId": "B",
      |  "courseName": "B course",
      |  "courseType": "Lecture",
      |  "startDate": "2020-06-30",
      |  "endDate": "2020-06-30",
      |  "ects": 0,
      |  "lecturerId": "string",
      |  "maxParticipants": 120,
      |  "currentParticipants": 0,
      |  "courseLanguage": "German",
      |  "courseDescription": "string"
      |}""".stripMargin

  /** Tests only working if the whole instance is started */
  "HyperLedgerService service" should {

    "read list of all courses" in {
      client.read("getAllCourses").invoke(List()).map { answer => {
        answer should ===("[]")
      }}
    }

    "not read a non-existing course" in {
      client.read("getCourseById").invoke(List("invalidID")).map { answer =>
        answer should ===("null")
      }
    }

    "write a course" in {
      client.write("addCourse").invoke(List(courseA)).map { answer =>
        answer should ===(Done)
      }
    }

    "not write a ill-formatted course" in {
      client.write("addCourse").invoke(List(courseInvalid)).failed.map { answer =>
        answer shouldBe a [Exception]
      }
    }

    "read a course" in {
      client.read("getCourseById").invoke(List("A")).map { answer =>
        answer should ===(courseA)
      }
    }

    "not write a non json" in {
      client.write("addCourse").invoke(List("invalid")).failed.map { answer =>
        answer shouldBe a [Exception]
      }
    }

    "delete a course" in {
      client.write("deleteCourseById").invoke(List("validID")).map { answer =>
        answer should ===(Done)
      }
    }

    "not delete a non-existing course" in {
      client.write("deleteCourseById").invoke(List("invalidID")).map { answer =>
        answer should ===(Done)
        //Currently HL returns "null", but Lagoms only considere Exceptions as failure
      }
    }

  }
}
