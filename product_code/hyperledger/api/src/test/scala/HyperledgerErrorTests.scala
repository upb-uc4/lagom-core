
import java.nio.file.Paths

import de.upb.cs.uc4.hyperledger.ConnectionManager
import de.upb.cs.uc4.hyperledger.exceptions.TransactionErrorException
import de.upb.cs.uc4.hyperledger.traits.ChaincodeActionsTrait
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class HyperledgerErrorTests extends AnyWordSpec with Matchers with BeforeAndAfterEach {

  val connectionManager = ConnectionManager(
    Paths.get(getClass.getResource("/connection_profile.yaml").toURI),
    Paths.get(getClass.getResource("/wallet/").toURI))
  var chaincodeConnection: ChaincodeActionsTrait = null

  override def beforeEach() {
    chaincodeConnection = connectionManager.createConnection()
  }

  override def afterEach() {
    chaincodeConnection.close()
  }


  "The ScalaAPI EvaluateTransaction" when {
    "asked for invalid transactions" should {
      "throw TransactionErrorException for empty transactionId " in {
        // test action
        val result = intercept[TransactionErrorException](() -> chaincodeConnection.evaluateTransaction("getCourseById", "1"))
        result.transactionId should ===("getCourseById")
        result.errorCode should ===(0)
        result.errorDetail should ===("Returned null.")
      }
      "throw TransactionErrorException for wrong transactionId during update " in {
        // test action
        val result = intercept[TransactionErrorException](() -> chaincodeConnection.submitTransaction("updateCourseById", "1", TestData.invalidCourseData(null)))
        result.transactionId should ===("updateCourseById")
        result.errorCode should ===(0)
        result.errorDetail should ===("Course ID and ID in path do not match")
      }
    }
  }
}