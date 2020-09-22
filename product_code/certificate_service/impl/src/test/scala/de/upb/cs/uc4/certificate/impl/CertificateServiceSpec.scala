package de.upb.cs.uc4.certificate.impl

import java.util.Calendar

import akka.stream.scaladsl.Source
import com.lightbend.lagom.scaladsl.api.transport.RequestHeader
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.{ ServiceTest, TestTopicComponents }
import de.upb.cs.uc4.authentication.model.{ AuthenticationRole, JsonUsername }
import de.upb.cs.uc4.user.DefaultTestUsers
import de.upb.cs.uc4.user.api.UserService
import io.jsonwebtoken.{ Jwts, SignatureAlgorithm }
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

/** Tests for the CertificateService
  * All tests need to be started in the defined order
  */
class CertificateServiceSpec extends AsyncWordSpec with Matchers with BeforeAndAfterAll with Eventually with DefaultTestUsers {

  private val server = ServiceTest.startServer(
    ServiceTest.defaultSetup
      .withJdbc()
  ) { ctx =>
      new CertificateApplication(ctx) with LocalServiceLocator with TestTopicComponents
    }

  val client: UserService = server.serviceClient.implement[UserService]

  val deletionTopic: Source[JsonUsername, _] = client.userDeletedTopic().subscribe.atMostOnceSource

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
}
