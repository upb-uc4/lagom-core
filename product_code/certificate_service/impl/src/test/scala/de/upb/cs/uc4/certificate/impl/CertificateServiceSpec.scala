package de.upb.cs.uc4.certificate.impl

import java.nio.file.Path
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.{ ProducerStub, ProducerStubFactory, ServiceTest, TestTopicComponents }
import de.upb.cs.uc4.certificate.api.CertificateService
import de.upb.cs.uc4.certificate.model.{ EncryptedPrivateKey, PostMessageCSR }
import de.upb.cs.uc4.hyperledger.BuildInfo
import de.upb.cs.uc4.hyperledger.utilities.traits.{ EnrollmentManagerTrait, RegistrationManagerTrait }
import de.upb.cs.uc4.shared.client.exceptions.{ DetailedError, ErrorType, GenericError, UC4Exception }
import de.upb.cs.uc4.shared.client.kafka.EncryptionContainer
import de.upb.cs.uc4.shared.server.UC4SpecUtils
import de.upb.cs.uc4.user.api.UserService
import de.upb.cs.uc4.user.model.{ JsonUserData, Role, Usernames }
import de.upb.cs.uc4.user.{ DefaultTestUsers, UserServiceStub }
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{ Seconds, Span }
import org.scalatest.wordspec.AsyncWordSpec

/** Tests for the CertificateService */
class CertificateServiceSpec extends AsyncWordSpec
  with UC4SpecUtils with Matchers with BeforeAndAfterAll with Eventually with DefaultTestUsers {

  private var creationStub: ProducerStub[EncryptionContainer] = _ //EncryptionContainer[Usernames]
  private var deletionStub: ProducerStub[EncryptionContainer] = _ //EncryptionContainer[Usernames]

  private val server = ServiceTest.startServer(
    ServiceTest.defaultSetup
      .withJdbc()
  ) { ctx =>
      new CertificateApplication(ctx) with LocalServiceLocator with TestTopicComponents {

        override lazy val enrollmentManager: EnrollmentManagerTrait = new EnrollmentManagerTrait {

          override def enrollSecure(caURL: String, caCert: Path, enrollmentID: String, enrollmentSecret: String,
              csr_pem: String, adminName: String, adminWalletPath: Path, channel: String,
              chaincode: String, networkDescriptionPath: Path): String = s"certificate for $enrollmentID"

          override def enroll(caURL: String, caCert: Path, walletPath: Path, enrollmentID: String, enrollmentSecret: String,
              organisationId: String, channel: String, chaincode: String, networkDescriptionPath: Path): String = s"certificate for $enrollmentID"
        }

        override lazy val registrationManager: RegistrationManagerTrait =
          (_: String, _: Path, username: String, _: String, _: Path, _: String, _: Integer, _: String) => s"$username-secret"

        lazy val stubFactory = new ProducerStubFactory(actorSystem, materializer)
        lazy val internCreationStub: ProducerStub[EncryptionContainer] =
          stubFactory.producer[EncryptionContainer](UserService.ADD_TOPIC_NAME)
        creationStub = internCreationStub
        lazy val internDeletionStub: ProducerStub[EncryptionContainer] =
          stubFactory.producer[EncryptionContainer](UserService.DELETE_TOPIC_PRECISE_NAME)
        deletionStub = internDeletionStub

        // Create a userService with ProducerStub as topic
        override lazy val userService: UserServiceStubWithTopic = new UserServiceStubWithTopic(internCreationStub, internDeletionStub)
      }
    }

  val client: CertificateService = server.serviceClient.implement[CertificateService]

  override protected def afterAll(): Unit = server.stop()

  "The CertificateService" should {
    "register a user and get enrollmentId" in {
      val username = "student00"
      val container = server.application.kafkaEncryptionUtility.encrypt(Usernames(username, username + "enroll", Role.Student))
      creationStub.send(container)

      eventually(timeout(Span(30, Seconds))) {
        client.getEnrollmentId(username).handleRequestHeader(addAuthorizationHeader()).invoke().map {
          answer => answer.id should ===(username + "enroll")
        }
      }
    }

    "reset certificate state on user deletion" in {
      val username = "student005"
      val container = server.application.kafkaEncryptionUtility.encrypt(Usernames(username, username + "enroll", Role.Student))
      val deletionContainer = server.application.kafkaEncryptionUtility.encrypt(JsonUserData("student005", student0.role, forceDelete = true))
      creationStub.send(container)

      eventually(timeout(Span(30, Seconds))) {
        client.getEnrollmentId(username).handleRequestHeader(addAuthorizationHeader()).invoke().map {
          answer => answer.id should ===(username + "enroll")
        }
      }.flatMap { _ =>
        deletionStub.send(deletionContainer)
        eventually(timeout(Span(30, Seconds))) {
          client.getEnrollmentId(username).handleRequestHeader(addAuthorizationHeader()).invoke().failed.map {
            answer => answer.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.KeyNotFound)
          }
        }
      }
    }

    "return an error when fetching the enrollmentId of a non-existing user" in {
      val username = "student01"
      client.getEnrollmentId(username).handleRequestHeader(addAuthorizationHeader()).invoke().failed.map {
        answer => answer.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.KeyNotFound)
      }
    }

    "enroll a user and get the certificate" in {
      val username = "student02"
      val enrollmentId = username + "enroll"
      val container = server.application.kafkaEncryptionUtility.encrypt(Usernames(username, enrollmentId, Role.Student))
      creationStub.send(container)

      client.setCertificate(username).handleRequestHeader(addAuthorizationHeader(username))
        .invoke(PostMessageCSR("csr", EncryptedPrivateKey("", "", ""))).flatMap {
          answer => answer.certificate should ===(s"certificate for " + enrollmentId)
        }
    }

    "successfully  fetch  the certificate of an enrolled user" in {
      val username = "student02a"
      val enrollmentId = username + "enroll"
      val container = server.application.kafkaEncryptionUtility.encrypt(Usernames(username, enrollmentId))
      creationStub.send(container)

      client.setCertificate(username).handleRequestHeader(addAuthorizationHeader(username))
        .invoke(PostMessageCSR("csr", EncryptedPrivateKey("", "", ""))).flatMap {
          _ =>
            client.getCertificate(username).handleRequestHeader(addAuthorizationHeader(username))
              .invoke().flatMap {
                answer => answer.certificate should ===(s"certificate for " + enrollmentId)
              }
        }
    }

    "return an error when fetching the certificate of a non-enrolled user" in {
      val username = "student03"
      val container = server.application.kafkaEncryptionUtility.encrypt(Usernames(username, username + "enroll", Role.Student))
      creationStub.send(container)

      client.getCertificate(username).handleRequestHeader(addAuthorizationHeader())
        .invoke().failed.map {
          answer => answer.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.KeyNotFound)
        }
    }

    "return an error when a user enrolls twice" in {
      val username = "student04"
      val container = server.application.kafkaEncryptionUtility.encrypt(Usernames(username, username + "enroll", Role.Student))
      creationStub.send(container)

      client.setCertificate(username).handleRequestHeader(addAuthorizationHeader(username))
        .invoke(PostMessageCSR("csr", EncryptedPrivateKey("", "", ""))).flatMap { _ =>

          client.setCertificate(username).handleRequestHeader(addAuthorizationHeader(username))
            .invoke(PostMessageCSR("csr", EncryptedPrivateKey("", "", ""))).failed.map { answer =>
              answer.asInstanceOf[UC4Exception].possibleErrorResponse.asInstanceOf[GenericError]
                .`type` should ===(ErrorType.AlreadyEnrolled)
            }
        }
    }

    "return an error when a user enrolls another user" in {
      val username = "student05"
      val container = server.application.kafkaEncryptionUtility.encrypt(Usernames(username, username, Role.Student))
      creationStub.send(container)

      client.setCertificate(username).handleRequestHeader(addAuthorizationHeader("not" + username))
        .invoke(PostMessageCSR("csr", EncryptedPrivateKey("", "", ""))).failed.map { answer =>
          answer.asInstanceOf[UC4Exception].possibleErrorResponse.asInstanceOf[GenericError]
            .`type` should ===(ErrorType.OwnerMismatch)
        }
    }

    "return an error enrollment uses an invalid key object" in {
      val username = "student06"
      val container = server.application.kafkaEncryptionUtility.encrypt(Usernames(username, username, Role.Student))
      creationStub.send(container)

      client.setCertificate(username).handleRequestHeader(addAuthorizationHeader(username))
        .invoke(PostMessageCSR("csr", EncryptedPrivateKey("not", "valid", ""))).failed.map { answer =>
          answer.asInstanceOf[UC4Exception].possibleErrorResponse.asInstanceOf[DetailedError].invalidParams
            .map(_.name) should contain("encryptedPrivateKey")
        }
    }

    "return an error enrollment uses an invalid csr object" in {
      val username = "student065"
      val container = server.application.kafkaEncryptionUtility.encrypt(Usernames(username, username + "enroll", Role.Student))
      creationStub.send(container)

      client.setCertificate(username).handleRequestHeader(addAuthorizationHeader(username))
        .invoke(PostMessageCSR("", EncryptedPrivateKey("", "", ""))).failed.map { answer =>
          answer.asInstanceOf[UC4Exception].possibleErrorResponse.asInstanceOf[DetailedError].invalidParams
            .map(_.name) should contain("certificateSigningRequest")
        }
    }

    "enroll a user and get private key" in {
      val username = "student07"
      val container = server.application.kafkaEncryptionUtility.encrypt(Usernames(username, username + "enroll", Role.Student))
      creationStub.send(container)

      client.setCertificate(username).handleRequestHeader(addAuthorizationHeader(username))
        .invoke(PostMessageCSR("csr", EncryptedPrivateKey("private", "iv", "salt"))).flatMap { _ =>

          client.getPrivateKey(username).handleRequestHeader(addAuthorizationHeader(username))
            .invoke().map {
              answer => answer should ===(EncryptedPrivateKey("private", "iv", "salt"))
            }
        }
    }

    "return an error when fetching the private key of another user" in {
      val username = "student08"
      val container = server.application.kafkaEncryptionUtility.encrypt(Usernames(username, username + "enroll", Role.Student))
      creationStub.send(container)

      client.setCertificate(username).handleRequestHeader(addAuthorizationHeader(username))
        .invoke(PostMessageCSR("csr", EncryptedPrivateKey("private", "iv", "salt"))).flatMap { _ =>

          client.getPrivateKey(username).handleRequestHeader(addAuthorizationHeader(username + "not"))
            .invoke().failed.map { answer =>
              answer.asInstanceOf[UC4Exception].possibleErrorResponse.asInstanceOf[GenericError]
                .`type` should ===(ErrorType.OwnerMismatch)
            }
        }
    }

    "return an error when fetching the private key of a non-enrolled user" in {
      val username = "student09"
      val container = server.application.kafkaEncryptionUtility.encrypt(Usernames(username, username + "enroll", Role.Student))
      creationStub.send(container)

      client.getPrivateKey(username).handleRequestHeader(addAuthorizationHeader(username))
        .invoke().failed.map { answer =>
          answer.asInstanceOf[UC4Exception].possibleErrorResponse.asInstanceOf[GenericError]
            .`type` should ===(ErrorType.NotEnrolled)
        }
    }
  }
}

class UserServiceStubWithTopic(creationStub: ProducerStub[EncryptionContainer], deletionStub: ProducerStub[EncryptionContainer]) extends UserServiceStub {

  override def userCreationTopic(): Topic[EncryptionContainer] = creationStub.topic

  override def userDeletionTopicPrecise(): Topic[EncryptionContainer] = deletionStub.topic

}
