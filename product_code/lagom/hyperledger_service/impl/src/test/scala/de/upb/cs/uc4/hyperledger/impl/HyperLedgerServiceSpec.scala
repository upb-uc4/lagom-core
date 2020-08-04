package de.upb.cs.uc4.hyperledger.impl

import akka.Done
import com.lightbend.lagom.scaladsl.api.transport.TransportErrorCode
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import de.upb.cs.uc4.hyperledger.api.HyperLedgerService
import de.upb.cs.uc4.hyperledger.traits.{ChaincodeActionsTrait, ConnectionManagerTrait}
import de.upb.cs.uc4.shared.client.exceptions.CustomException
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
      override lazy val connectionManager: ConnectionManagerTrait = () => new ChaincodeActionsTrait {

        override val contract_course = null;
        override def close(): Unit = {}

        override def internalSubmitTransaction(transactionId: String, params: String*): Array[Byte] = transactionId match {
          case "addCourse" =>
            params.head match {
              case `courseA` => "".getBytes
              case `courseB` => "".getBytes
              case `courseInvalid` => throw new Exception("""{"name":"50","detail":"ects must be a positive integer number"}""")
            }
          case "deleteCourseById" =>
            params.head match {
              case "A" => "".getBytes
              case "validID" => "".getBytes
              case "invalidID" => throw new Exception("Course not found.")
            }
          case "updateCourseById" =>
            params.head match {
              case "validID" => "".getBytes()
              case "invalidID" => throw new Exception("""{"name":"03","detail":"Course not found"}""")
            }
          case _ => throw new Exception("Undefined")
        }

        override def internalEvaluateTransaction(transactionId: String, params: String*): Array[Byte] = transactionId match {
          case "getAllCourses" => "[]".getBytes //empty
          //not empty = [courseA]
          case "getCourseById" =>
            params.head match {
              case "A" => courseA.getBytes
              case "B" => courseB.getBytes
              case "invalidID" => throw new Exception("Course not found.")
            }
          case _ => throw new Exception("Undefined")
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

    "read empty list of all courses" in {
      client.read("getAllCourses").invoke(List()).map { answer => {
        answer should ===("[]")
      }}
    }

    "not read a non-existing course" in {
      client.read("getCourseById").invoke(List("invalidID")).failed.map { answer =>
        answer.asInstanceOf[CustomException].getErrorCode should ===(TransportErrorCode(404, 1008, "Policy Violation/Not Found"))
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
      client.write("deleteCourseById").invoke(List("A")).map { answer =>
        answer should ===(Done)
      }
    }

    "not delete a non-existing course" in {
      client.write("deleteCourseById").invoke(List("invalidID")).failed.map { answer =>
        answer shouldBe a [Exception]
      }
    }

  }
}
