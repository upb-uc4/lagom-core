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
      creationStub.send(Usernames("student00", "student00"))

      eventually(timeout(Span(30, Seconds))) {
        client.getEnrollmentId("student00").handleRequestHeader(addAuthorizationHeader()).invoke().map{
          answer => answer.id should ===("student00")
        }
      }
    }

    "enroll a user and get the certificate" in {
      creationStub.send(Usernames("student01", "student01"))

      client.setCertificate("student01").handleRequestHeader(addAuthorizationHeader("student01"))
        .invoke(PostMessageCSR("", EncryptedPrivateKey("", "", ""))).flatMap { _ =>

        client.getCertificate("student01").handleRequestHeader(addAuthorizationHeader("student01"))
          .invoke().map {
          answer => answer.certificate should ===(s"certificate for student01")
        }
      }
    }

    "enroll a user and get private key" in {
      creationStub.send(Usernames("student02", "student02"))

      client.setCertificate("student02").handleRequestHeader(addAuthorizationHeader("student02"))
        .invoke(PostMessageCSR("", EncryptedPrivateKey("private", "iv", "salt"))).flatMap { _ =>

        client.getPrivateKey("student02").handleRequestHeader(addAuthorizationHeader("student02"))
          .invoke().map {
          answer => answer should ===(EncryptedPrivateKey("private", "iv", "salt"))
        }
      }
    }
  }
}

class UserServiceStubWithTopic(creationStub: ProducerStub[Usernames]) extends UserServiceStub {

  override def userCreationTopic(): Topic[Usernames] = creationStub.topic

}
