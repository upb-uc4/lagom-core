import de.upb.cd.uc4.hyperledger.ConnectionManager
import org.scalatest.FunSuite

class ChaincodeConnectionObjectTests extends FunSuite {

  test("Check access per connection object") {
    // setup connection
    val chaincodeConnection = ConnectionManager.createConnection()

    // perform action
    try{
      //retrieve result on query
      val courses = chaincodeConnection.getCourses()

      // test result
      assert(courses != null, "courses returned was null")
    } finally {
      // close connection
      chaincodeConnection.close();
    }
  }

}
