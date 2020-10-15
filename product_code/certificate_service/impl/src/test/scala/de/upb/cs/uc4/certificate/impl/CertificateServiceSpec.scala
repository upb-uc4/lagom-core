package de.upb.cs.uc4.certificate.impl

import java.nio.file.Path
import java.util.Calendar

import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.transport.RequestHeader
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.{ ProducerStub, ProducerStubFactory, ServiceTest, TestTopicComponents }
import de.upb.cs.uc4.authentication.model.AuthenticationRole
import de.upb.cs.uc4.certificate.api.CertificateService
import de.upb.cs.uc4.certificate.model.{ EncryptedPrivateKey, PostMessageCSR }
import de.upb.cs.uc4.hyperledger.utilities.traits.{ EnrollmentManagerTrait, RegistrationManagerTrait }
import de.upb.cs.uc4.shared.client.exceptions.{ DetailedError, ErrorType, GenericError, UC4Exception }
import de.upb.cs.uc4.user.api.UserService
import de.upb.cs.uc4.user.model.Usernames
import de.upb.cs.uc4.user.{ DefaultTestUsers, UserServiceStub }
import io.jsonwebtoken.{ Jwts, SignatureAlgorithm }
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{ Seconds, Span }
import org.scalatest.wordspec.AsyncWordSpec

/** Tests for the CertificateService
  * All tests need to be started in the defined order
  */
class CertificateServiceSpec extends AsyncWordSpec with Matchers with BeforeAndAfterAll with Eventually with DefaultTestUsers {

  private var creationStub: ProducerStub[Usernames] = _

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
              organisationId: String, channel: String, chaincode: String, networkDescriptionPath: Path): Unit = {}
        }

        override lazy val registrationManager: RegistrationManagerTrait =
          (_: String, _: Path, username: String, _: String, _: Path, _: String, _: Integer, _: String) => s"$username-secret"

        lazy val stubFactory = new ProducerStubFactory(actorSystem, materializer)
        lazy val internCreationStub: ProducerStub[Usernames] =
          stubFactory.producer[Usernames](UserService.ADD_TOPIC_NAME)

        creationStub = internCreationStub

        // Create a userService with ProducerStub as topic
        override lazy val userService: UserServiceStubWithTopic = new UserServiceStubWithTopic(internCreationStub)
      }
    }

  val client: CertificateService = server.serviceClient.implement[CertificateService]

  override protected def afterAll(): Unit = server.stop()

  def addAuthorizationHeader(username: String = "admin"): RequestHeader => RequestHeader = { header =>

    var role = AuthenticationRole.Admin

    if (username.contains("student")) {
      role = AuthenticationRole.Student
    }
    else if (username.contains("lecturer")) {
      role = AuthenticationRole.Lecturer
    }

    val time = Calendar.getInstance()
    time.add(Calendar.DATE, 1)

    val token =
      Jwts.builder()
        .setSubject("login")
        .setExpiration(time.getTime)
        .claim("username", username)
        .claim("authenticationRole", role.toString)
        .signWith(SignatureAlgorithm.HS256, "changeme")
        .compact()

    header.withHeader("Cookie", s"login=$token")
  }

  "The CertificateService" should {
    "register a user and get enrollmentId" in {
      val username = "student00"
      creationStub.send(Usernames(username, username + "enroll"))

      eventually(timeout(Span(30, Seconds))) {
        client.getEnrollmentId(username).handleRequestHeader(addAuthorizationHeader()).invoke().map {
          answer => answer.id should ===(username + "enroll")
        }
      }
    }

    "return an error when fetching the enrollmentId of a non-existing user" in {
      val username = "student01"
      client.getEnrollmentId(username).handleRequestHeader(addAuthorizationHeader()).invoke().failed.map {
        answer => answer.asInstanceOf[UC4Exception].errorCode.http should ===(404)
      }
    }

    "enroll a user and get the certificate" in {
      val username = "student02"
      val enrollmentId = username + "enroll"
      creationStub.send(Usernames(username, enrollmentId))

      client.setCertificate(username).handleRequestHeader(addAuthorizationHeader(username))
        .invoke(PostMessageCSR("csr", EncryptedPrivateKey("", "", ""))).flatMap { _ =>

          client.getCertificate(username).handleRequestHeader(addAuthorizationHeader())
            .invoke().map {
              answer => answer.certificate should ===(s"certificate for " + enrollmentId)
            }
        }
    }

    "return an error when fetching the certificate of a non-enrolled user" in {
      val username = "student03"
      creationStub.send(Usernames(username, username + "enroll"))

      client.getCertificate(username).handleRequestHeader(addAuthorizationHeader())
        .invoke().failed.map {
          answer => answer.asInstanceOf[UC4Exception].errorCode.http should ===(404)
        }
    }

    "return an error when a user enrolls twice" in {
      val username = "student04"
      creationStub.send(Usernames(username, username + "enroll"))

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
      creationStub.send(Usernames(username, username))

      client.setCertificate(username).handleRequestHeader(addAuthorizationHeader("not" + username))
        .invoke(PostMessageCSR("csr", EncryptedPrivateKey("", "", ""))).failed.map { answer =>
          answer.asInstanceOf[UC4Exception].possibleErrorResponse.asInstanceOf[GenericError]
            .`type` should ===(ErrorType.OwnerMismatch)
        }
    }

    "return an error enrollment uses an invalid key object" in {
      val username = "student06"
      creationStub.send(Usernames(username, username))

      client.setCertificate(username).handleRequestHeader(addAuthorizationHeader(username))
        .invoke(PostMessageCSR("csr", EncryptedPrivateKey("not", "valid", ""))).failed.map { answer =>
          answer.asInstanceOf[UC4Exception].possibleErrorResponse.asInstanceOf[DetailedError].invalidParams
            .map(_.name) should contain("encryptedPrivateKey")
        }
    }

    "return an error enrollment uses an invalid csr object" in {
      val username = "student065"
      creationStub.send(Usernames(username, username))

      client.setCertificate(username).handleRequestHeader(addAuthorizationHeader(username))
        .invoke(PostMessageCSR("", EncryptedPrivateKey("", "", ""))).failed.map { answer =>
          answer.asInstanceOf[UC4Exception].possibleErrorResponse.asInstanceOf[DetailedError].invalidParams
            .map(_.name) should contain("certificateSigningRequest")
        }
    }

    "enroll a user and get private key" in {
      val username = "student07"
      creationStub.send(Usernames(username, username + "enroll"))

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
      creationStub.send(Usernames(username, username + "enroll"))

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
      creationStub.send(Usernames(username, username + "enroll"))

      client.getPrivateKey(username).handleRequestHeader(addAuthorizationHeader(username))
        .invoke().failed.map { answer =>
          answer.asInstanceOf[UC4Exception].possibleErrorResponse.asInstanceOf[GenericError]
            .`type` should ===(ErrorType.NotEnrolled)
        }
    }
  }
}

class UserServiceStubWithTopic(creationStub: ProducerStub[Usernames]) extends UserServiceStub {

  override def userCreationTopic(): Topic[Usernames] = creationStub.topic

}
