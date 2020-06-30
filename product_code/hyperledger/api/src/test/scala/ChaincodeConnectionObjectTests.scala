
import de.upb.cs.uc4.hyperledger.ConnectionManager
import de.upb.cs.uc4.hyperledger.traits.ChaincodeTrait
import org.scalatest.FunSuite

import scala.util.{Success, Using}

class ChaincodeConnectionObjectTests extends FunSuite {

  test("Check command getAllCourses") {
    // setup connection
    val chaincodeConnection = ConnectionManager.createConnection()

    // perform action
    try{
      //retrieve result on query
      val courses = chaincodeConnection.getAllCourses()

      // test result
      assert(courses != null, "courses returned was null")
    } finally {
      // close connection
      chaincodeConnection.close();
    }
  }

  test ("Check full walk-through"){
    Using(ConnectionManager.createConnection()) { chaincodeConnection: ChaincodeTrait =>
      // initial courses
      val courses = chaincodeConnection.getAllCourses()
      assert(courses != null, "Get All courses returned null")
      println("Courses: " + courses)

      // get first initialCourse
      val initialExampleCourse = chaincodeConnection.getCourseById("courseId")
      assert(initialExampleCourse != null, "GetCourseById(courseId) returned null")
      println("initialExampleCourse: " + initialExampleCourse)

      // add new course
      val exampleCourseData = "{\"lecturerId\":\"Mustermann\",\"courseName\":\"IQC\",\"courseType\":\"Lecture\",\"ects\":8,\"courseLanguage\":\"English\",\"endDate\":\"01.01.1999\",\"currentParticipants\":20,\"courseId\":\"42\",\"courseDescription\":\"Fun new course\",\"startDate\":\"01.01.1998\",\"maxParticipants\":80}"
      val addResult = chaincodeConnection.addCourse(exampleCourseData)
      assert(addResult != null, "Add newCourse returned null")
      println("AddNew Result: " + addResult)

      // Check AddNew worked as expected
      val readNewCourse = chaincodeConnection.getCourseById("42")
      assert(readNewCourse != null, "GetCourseById(42) returned null")
      println("newCourse read: " + readNewCourse)
      assert(readNewCourse.equals(exampleCourseData), "retrieved info did not match inserted data")

      // delete new course
      val deleteCourseResult = chaincodeConnection.deleteCourseById("42")
      assert(deleteCourseResult != null, "DeleteCourseById(42) returned null")
      println("deleteCourseResult: " + deleteCourseResult)
      val tryGetRemovedCourse = chaincodeConnection.getCourseById("42")
      println("removeResult: " + tryGetRemovedCourse)
      assert(tryGetRemovedCourse == null, "tryGetRemovedCourse should return null")

      // update new course
      val updateOldCouresResult = chaincodeConnection.updateCourseById("courseId", exampleCourseData)
      assert(updateOldCouresResult != null, "updateCouresById returned null")
      println("updateOldCouresResult: " + updateOldCouresResult)
    }
  }

}
