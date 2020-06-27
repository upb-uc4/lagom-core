import de.upb.cd.uc4.hyperledger.ChaincodeQuickAccess
import org.scalatest.FunSuite

class ChaincodeQuickAccessTests extends FunSuite {

  test("Check listing all courses") {
    // retrieve quickAccess result
    val result = ChaincodeQuickAccess.getAllCourses();

    // debug help
    println(result)

    // perform tests
    assert(result != null, "result was null")
    assert(result.isSuccess, "Query failed")
  }

}
