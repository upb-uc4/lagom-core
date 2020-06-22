import org.scalatest.FunSuite
import de.upb.cs.uc4.hyperledger.access.ChaincodeAccess

class ChaincodeTests extends FunSuite {

  test("Check listing all courses") {
    val result = ChaincodeAccess.getCourses();
    println(result)
    assert(result != null, "result was null")
  }

}
