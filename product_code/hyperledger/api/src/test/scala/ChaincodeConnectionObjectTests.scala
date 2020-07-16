
import de.upb.cs.uc4.hyperledger.{ChaincodeQuickAccess, ConnectionManager}
import de.upb.cs.uc4.hyperledger.traits.ChaincodeTrait
import org.scalatest.wordspec.AnyWordSpec

import scala.util.{Success, Using}

class ChaincodeConnectionObjectTests extends AnyWordSpec {

  val connectionManager = ConnectionManager()

  "A ChaincodeConnection" when {
    "initialized" should {
      "Allow for getCALlCourses" in {
        // setup connection
        val chaincodeConnection = connectionManager.createConnection()

        // perform action
        try {
          //retrieve result on query
          val courses = chaincodeConnection.getAllCourses()

          // test result
          assert(courses != null, "courses returned was null")
        } finally {
          // close connection
          chaincodeConnection.close();
        }
      }

      "Allow a full walkthrough" in {
        val testResult = Using(connectionManager.createConnection()) { chaincodeConnection: ChaincodeTrait =>
          // initial courses
          val courses = chaincodeConnection.getAllCourses()
          assert(courses != null, "Get All courses returned null")
          println("Courses: " + courses)

          // add new course
          val testCourseId = "41"
          val exampleCourseData = "{\"courseId\":\"" + testCourseId + "\",\"courseName\":\"IQC\",\"courseType\":\"Lecture\",\"startDate\":\"1998-01-01\",\"endDate\":\"1999-01-01\",\"ects\":7,\"lecturerId\":\"Mustermann\",\"maxParticipants\":80,\"currentParticipants\":20,\"courseLanguage\":\"English\",\"courseDescription\":\"Fun new course\"}"
          val addResult = chaincodeConnection.addCourse(exampleCourseData)
          assert(addResult != null, "Add newCourse returned null")
          assert(addResult.equals(""), "Add newCourse returned error: " + addResult)
          println("AddNew Result: " + addResult)

          // Check AddNew worked as expected READ COURSE
          val readNewCourse = chaincodeConnection.getCourseById(testCourseId)
          assert(readNewCourse != null, "getCourseById(" + testCourseId + ") returned null")
          println("newCourse read: " + readNewCourse)
          println("example data: " + exampleCourseData)
          assert(readNewCourse.equals({
            exampleCourseData
          }.toString()), "retrieved info did not match inserted data")

          // delete new course
          val deleteCourseResult = chaincodeConnection.deleteCourseById(testCourseId)
          assert(deleteCourseResult != null, "DeleteCourseById(" + testCourseId + ") returned null")
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
          assert(updateAddCourseResult.equals(""), "Add newCourse returned error: " + addResult)
          println("AddNew Result: " + updateAddCourseResult)
          // update
          val exampleCourseData3 = "{\"courseId\":\"" + testUpdateCourseId + "\",\"courseName\":\"Intro to Quantum\",\"courseType\":\"Lecture\",\"startDate\":\"1998-01-01\",\"endDate\":\"1999-01-01\",\"ects\":7,\"lecturerId\":\"Mustermann\",\"maxParticipants\":80,\"currentParticipants\":20,\"courseLanguage\":\"English\",\"courseDescription\":\"Fun new course\"}"
          val updateCouresResult = chaincodeConnection.updateCourseById(testUpdateCourseId, exampleCourseData3)
          assert(updateCouresResult != null, "updateCouresById returned null")
          assert(updateCouresResult.equals(""), "Update course returned error: " + updateCouresResult)
          println("updateOldCouresResult: " + updateCouresResult)
        }

        val allCoursesAfter = ChaincodeQuickAccess.getAllCourses()
        println("All Courses after test: " + allCoursesAfter)

        assert(testResult.isSuccess, "Error during test.")
      }
    }
  }
}