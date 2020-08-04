
import java.nio.file.Paths

import de.upb.cs.uc4.hyperledger.ConnectionManager
import de.upb.cs.uc4.hyperledger.exceptions.InvalidCallException
import de.upb.cs.uc4.hyperledger.traits.ChaincodeActionsTrait
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class InvalidCallTests extends AnyWordSpec with Matchers with BeforeAndAfterEach {

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


  "The ScalaAPI SubmitTransaction" when {
    "asked for invalid transactions" should {
      "throw InvalidTrasactionException for empty transactionId " in {
        // test action
        val result = intercept[InvalidCallException](() -> chaincodeConnection.submitTransaction(""))
        result.transactionId should ===("")
        result.detail should ===("The transaction is not defined.")
      }
      "throw InvalidTrasactionException for undefined transactionId " in {
        // test action
        val result = intercept[InvalidCallException](() -> chaincodeConnection.submitTransaction("undefined"))
        result.transactionId should ===("undefined")
        result.detail should ===("The transaction is not defined.")
      }
      "throw InvalidTrasactionException for evaluation transactionId " in {
        // test action
        val result = intercept[InvalidCallException](() -> chaincodeConnection.submitTransaction("getAllCourses"))
        result.transactionId should ===("getAllCourses")
        result.detail should ===("The transaction is not defined.")
      }
    }

    "accessed with invalid parameterCount" should {
      "throw InvalidTrasactionException for missing parameters" in {
        // test action
        val result = intercept[InvalidCallException](() -> chaincodeConnection.submitTransaction("addCourse"))
        result.transactionId should ===("addCourse")
        result.detail should ===("The transaction was invoked with the wrong amount of parameters. Expected: " + 1 + " Actual: " + 0)
      }
      "throw InvalidTrasactionException for to many parameters" in {
        // test action
        val result = intercept[InvalidCallException](() -> chaincodeConnection.submitTransaction("addCourse", TestData.exampleCourseData("1"), TestData.exampleCourseData("2")))
        result.transactionId should ===("addCourse")
        result.detail should ===("The transaction was invoked with the wrong amount of parameters. Expected: " + 1 + " Actual: " + 2)
      }
    }
  }

  "The ScalaAPI EvaluateTransaction" when {
    "asked for invalid transactions" should {
      "throw InvalidTrasactionException for empty transactionId " in {
        // test action
        val result = intercept[InvalidCallException](() -> chaincodeConnection.evaluateTransaction(""))
        result.transactionId should ===("")
        result.detail should ===("The transaction is not defined.")
      }
      "throw InvalidTrasactionException for undefined transactionId " in {
        // test action
        val result = intercept[InvalidCallException](() -> chaincodeConnection.evaluateTransaction("undefined"))
        result.transactionId should ===("undefined")
        result.detail should ===("The transaction is not defined.")
      }
      "throw InvalidTrasactionException for evaluation transactionId " in {
        // test action
        val result = intercept[InvalidCallException](() -> chaincodeConnection.evaluateTransaction("addCourse"))
        result.transactionId should ===("addCourse")
        result.detail should ===("The transaction is not defined.")
      }
    }

    "accessed with invalid parameterCount" should {
      "throw InvalidTrasactionException for missing parameters" in {
        // test action
        val result = intercept[InvalidCallException](() -> chaincodeConnection.evaluateTransaction("getCourseById"))
        result.transactionId should ===("getCourseById")
        result.detail should ===("The transaction was invoked with the wrong amount of parameters. Expected: " + 1 + " Actual: " + 0)
      }
      "throw InvalidTrasactionException for to many parameters" in {
        // test action
        val result = intercept[InvalidCallException](() -> chaincodeConnection.evaluateTransaction("getAllCourses", TestData.exampleCourseData("1")))
        result.transactionId should ===("getAllCourses")
        result.detail should ===("The transaction was invoked with the wrong amount of parameters. Expected: " + 0 + " Actual: " + 1)
      }
    }
  }
}