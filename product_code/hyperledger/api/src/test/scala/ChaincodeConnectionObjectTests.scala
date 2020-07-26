
import de.upb.cs.uc4.hyperledger.exceptions.InvalidTransactionException
import de.upb.cs.uc4.hyperledger.traits.ChaincodeActionsTrait
import de.upb.cs.uc4.hyperledger.{ChaincodeQuickAccess, ConnectionManager}
import org.scalatest.Succeeded
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.util.{Success, Using}

class ChaincodeConnectionObjectTests extends AnyWordSpec with Matchers {

  val connectionManager = ConnectionManager()

  "A ChaincodeConnection" when {
    "accessed with wrong transaction id" should {
      "throw InvalidTrasactionException for empty transactionId " in {
        // setup connection
        val chaincodeConnection = connectionManager.createConnection()

        // test action
        val result = intercept[InvalidTransactionException](() -> chaincodeConnection.evaluateTransaction(""))

        // close connection
        chaincodeConnection.close()
      }

      "throw InvalidTrasactionException for wrong type of transaction (submit)" in {
        // setup connection
        val chaincodeConnection = connectionManager.createConnection()

        // test action
        val result = intercept[InvalidTransactionException](() -> chaincodeConnection.submitTransaction("getAllCourses"))

        // close connection
        chaincodeConnection.close()
      }

      "throw InvalidTrasactionException for wrong type of transaction (evaluate)" in {
        // setup connection
        val chaincodeConnection = connectionManager.createConnection()

        // test action
        val result = intercept[InvalidTransactionException](() -> chaincodeConnection.evaluateTransaction("addCourse"))

        // close connection
        chaincodeConnection.close()
      }
    }

    "accessed with missing parameters" should {
      "throw an unspecified exception" in {
        // setup connection
        val chaincodeConnection = connectionManager.createConnection()

        // test action
        val result = intercept[Exception](() -> chaincodeConnection.evaluateTransaction("addCourse"))

        // close connection
        chaincodeConnection.close()
      }
    }

    "accessed as expected" should {
      "allow for getAllCourses" in {
        // setup connection
        val chaincodeConnection = connectionManager.createConnection()

        // perform action
        try {
          //retrieve result on query
          val courses = chaincodeConnection.getAllCourses()

          // test result
          courses should not be null
        } finally {
          // close connection
          chaincodeConnection.close();
        }
      }

      "allow a full walkthrough" in {
        val testResult = Using(connectionManager.createConnection()) { chaincodeConnection: ChaincodeActionsTrait =>
          // initial courses
          val getAllCourses = chaincodeConnection.getAllCourses()
          getAllCourses should not be null
          println("Courses: " + getAllCourses)

          // add new course
          val testCourseId = "41"
          val exampleCourseData = "{\"courseId\":\"" + testCourseId + "\",\"courseName\":\"IQC\",\"courseType\":\"Lecture\",\"startDate\":\"1998-01-01\",\"endDate\":\"1999-01-01\",\"ects\":7,\"lecturerId\":\"Mustermann\",\"maxParticipants\":80,\"currentParticipants\":20,\"courseLanguage\":\"English\",\"courseDescription\":\"Fun new course\"}"
          val addCourseResult = chaincodeConnection.addCourse(exampleCourseData)
          addCourseResult should not be null
          addCourseResult should equal ("")
          println("AddNew Result: " + addCourseResult)

          // Check AddNew worked as expected READ COURSE
          val readCourseResult = chaincodeConnection.getCourseById(testCourseId)
          readCourseResult should not be null
          println("newCourse read: " + readCourseResult)
          println("example data: " + exampleCourseData)
          readCourseResult should equal (exampleCourseData)

          // delete new course
          val deleteCourseResult = chaincodeConnection.deleteCourseById(testCourseId)
          deleteCourseResult should not equal null
          println("deleteCourseResult: " + deleteCourseResult)
          try {
            val tryGetRemovedCourse = chaincodeConnection.getCourseById(testCourseId)
            println("removeResult: " + tryGetRemovedCourse)
            assert(false, "The removec course did not throw an exception when trying to retrieve it.")
          } catch {
            case _: Throwable => println("Correctly threw an exception when getting deleted course.")
          }

          // update new course
          // add new course
          val testUpdateCourseId = "90"
          val exampleCourseData2 = "{\"courseId\":\"" + testUpdateCourseId + "\",\"courseName\":\"IQC\",\"courseType\":\"Lecture\",\"startDate\":\"1998-01-01\",\"endDate\":\"1999-01-01\",\"ects\":7,\"lecturerId\":\"Mustermann\",\"maxParticipants\":80,\"currentParticipants\":20,\"courseLanguage\":\"English\",\"courseDescription\":\"Fun new course\"}"
          val updateAddCourseResult = chaincodeConnection.addCourse(exampleCourseData2)
          updateAddCourseResult should equal ("")
          // update
          val exampleCourseData3 = "{\"courseId\":\"" + testUpdateCourseId + "\",\"courseName\":\"Intro to Quantum\",\"courseType\":\"Lecture\",\"startDate\":\"1998-01-01\",\"endDate\":\"1999-01-01\",\"ects\":7,\"lecturerId\":\"Mustermann\",\"maxParticipants\":80,\"currentParticipants\":20,\"courseLanguage\":\"English\",\"courseDescription\":\"Fun new course\"}"
          val updateCouresResult = chaincodeConnection.updateCourseById(testUpdateCourseId, exampleCourseData3)
          updateCouresResult should not be null
          updateCouresResult should equal ("")
        }

        val allCoursesAfter = ChaincodeQuickAccess.getAllCourses()
        println("All Courses after test: " + allCoursesAfter)

        println("TestResult : " + testResult)
        testResult should equal (Success(Succeeded))
      }
    }
  }
}