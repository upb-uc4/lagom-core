import de.upb.cs.uc4.hyperledger.ChaincodeQuickAccess
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ChaincodeQuickAccessTests extends AnyWordSpec with Matchers {

  "The ChainCodeQuickAccess" when {
    "accessed" should {
      "Allow for listing all courses" in {
        // retrieve quickAccess result
        val result = ChaincodeQuickAccess.getAllCourses();

        // debug help
        println(result)

        // perform tests
        result should not be null
      }
    }
  }
}
