package de.upb.cs.uc4.shared.server

import java.security.InvalidKeyException

import de.upb.cs.uc4.authentication.model.JsonUsername
import de.upb.cs.uc4.shared.client.JsonServiceVersion
import de.upb.cs.uc4.shared.client.exceptions.{ ErrorType, UC4Exception }
import de.upb.cs.uc4.shared.server.kafka.KafkaEncryptionUtility
import javax.crypto.AEADBadTagException
import org.scalatest.PrivateMethodTester
import org.scalatest.compatible.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

import scala.util.{ Failure, Success, Try }

class KafkaEncryptionUtilitySpec extends AsyncWordSpec with Matchers with PrivateMethodTester {

  private val kafkaEncryptionUtility = new KafkaEncryptionUtility(SecretsUtility.deriveKey("key", "salt"))
  private val kafkaEncryptionUtilityInvalid = new KafkaEncryptionUtility(null)

  "KafkaEncryptionUtility" should {

    "correctly encrypt and decrypt a valid object" in {
      val objectToEncrypt = JsonUsername("David")
      val encryptionContainer = kafkaEncryptionUtility.encrypt(objectToEncrypt)
      val decryptedObject = kafkaEncryptionUtility.decrypt[JsonUsername](encryptionContainer)
      decryptedObject should ===(objectToEncrypt)
    }

    "should not encrypt a null object" in {
      val objectToEncrypt: JsonUsername = null
      Try(kafkaEncryptionUtility.encrypt(objectToEncrypt)).isFailure shouldBe true
    }

    "should not decrypt an object with an altered class type" in {
      val objectToEncrypt = JsonUsername("David")
      val encryptionContainer = kafkaEncryptionUtility.encrypt(objectToEncrypt)
      val encryptionContainerAltered = encryptionContainer.copy(classType = "something")
      Try(kafkaEncryptionUtility.decrypt[JsonUsername](encryptionContainerAltered)) match {
        case Success(_)         => fail()
        case Failure(exception) => exception shouldBe a[AEADBadTagException]
      }
    }

    "should not decrypt an object with an unexpected class type" in {
      val objectToEncrypt = JsonUsername("David")
      val encryptionContainer = kafkaEncryptionUtility.encrypt(objectToEncrypt)
      Try(kafkaEncryptionUtility.decrypt[JsonServiceVersion](encryptionContainer)) match {
        case Success(_) => fail()
        case Failure(exception: UC4Exception) => exception.possibleErrorResponse.`type` should ===(ErrorType.KafkaDeserialization)
        case Failure(_) => fail()
      }
    }

    "should not decrypt an object with an altered data" in {
      val objectToEncrypt = JsonUsername("David")
      val encryptionContainer = kafkaEncryptionUtility.encrypt(objectToEncrypt)
      val alteredData = encryptionContainer.data
      alteredData(2) = 0
      val encryptionContainerAltered = encryptionContainer.copy(data = alteredData)
      Try(kafkaEncryptionUtility.decrypt[JsonUsername](encryptionContainerAltered)) match {
        case Success(_)         => fail()
        case Failure(exception) => exception shouldBe a[AEADBadTagException]
      }
    }

    "should not encrypt if key is invalid" in {
      val objectToEncrypt = JsonUsername("David")
      Try(kafkaEncryptionUtilityInvalid.encrypt(objectToEncrypt)) match {
        case Success(_)         => fail()
        case Failure(exception) => exception shouldBe a[InvalidKeyException]
      }
    }

    "should not decrypt if key is invalid" in {
      val objectToEncrypt = JsonUsername("David")
      val encryptionContainer = kafkaEncryptionUtility.encrypt(objectToEncrypt)
      Try(kafkaEncryptionUtilityInvalid.decrypt[JsonUsername](encryptionContainer)) match {
        case Success(_)         => fail()
        case Failure(exception) => exception shouldBe a[InvalidKeyException]
      }
    }

  }
}
