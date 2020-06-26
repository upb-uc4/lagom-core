package de.upb.cs.uc4.authentication.impl

import java.util.{Base64, Calendar}

import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.transport.{RequestHeader, TransportException}
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import de.upb.cs.uc4.authentication.api.AuthenticationService
import de.upb.cs.uc4.authentication.model.AuthenticationRole
import de.upb.cs.uc4.shared.ServiceCallFactory
import io.jsonwebtoken.{Jwts, SignatureAlgorithm}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

import scala.concurrent.Future

/** Tests for the AuthenticationService
  * All tests need to be started in the defined order
  */
class AuthenticationServiceSpec extends AsyncWordSpec with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  private val server = ServiceTest.startServer(
    ServiceTest.defaultSetup
      .withCassandra()
  ) { ctx =>
    new AuthenticationApplication(ctx) with LocalServiceLocator
  }

  private val client: AuthenticationService = server.serviceClient.implement[AuthenticationService]

  override protected def afterAll(): Unit = server.stop()

  def addLoginHeader(user: String, pw: String): RequestHeader => RequestHeader = { header =>
    header.withHeader("Authorization", "Basic " + Base64.getEncoder.encodeToString(s"$user:$pw".getBytes()))
  }

  def addJwtsHeader(jwts: String): RequestHeader => RequestHeader = { header =>
    header.withHeader("Authorization", s"Bearer $jwts")
  }

  /** Tests only working if the whole instance is started */
  "AuthenticationService service" should {

    "have the standard admin" in {
      client.login().handleRequestHeader(addLoginHeader("admin", "admin")).invoke().map{ answer =>
        answer shouldBe a [String]
      }
    }

    "have the standard lecturer" in {
      client.login().handleRequestHeader(addLoginHeader("lecturer", "lecturer")).invoke().map{ answer =>
        answer shouldBe a [String]
      }
    }

    "have the standard student" in {
      client.login().handleRequestHeader(addLoginHeader("student", "student")).invoke().map{ answer =>
        answer shouldBe a [String]
      }
    }

    "detect a wrong username" in {
      client.login().handleRequestHeader(addLoginHeader("studenta", "student")).invoke().failed.map{
        answer => answer.asInstanceOf[TransportException].errorCode.http should ===(401)
      }
    }

    "detect a wrong password" in {
      client.login().handleRequestHeader(addLoginHeader("admin", "student")).invoke().failed.map{
        answer => answer.asInstanceOf[TransportException].errorCode.http should ===(401)
      }
    }

    "detect that a user is not authorized" in {
      val now = Calendar.getInstance()
      now.add(Calendar.MINUTE, 10)
      val jws =
        Jwts.builder()
          .setSubject("authentication")
          .setExpiration(now.getTime)
          .claim("username", "student")
          .claim("AuthenticationRole", AuthenticationRole.Student.toString)
          .signWith(SignatureAlgorithm.HS256, "changeme")
          .compact()

      val serviceCall = ServiceCallFactory.authenticated[NotUsed, NotUsed](AuthenticationRole.Admin){
         _ => Future.successful(NotUsed)
      }(client, server.executionContext)

      serviceCall.handleRequestHeader(addJwtsHeader(jws)).invoke().failed.map{ answer =>
        answer.asInstanceOf[TransportException].errorCode.http should ===(403)
      }
    }

    "detect that a user is authorized" in {
      val now = Calendar.getInstance()
      now.add(Calendar.MINUTE, 10)
      val jws =
        Jwts.builder()
          .setSubject("authentication")
          .setExpiration(now.getTime)
          .claim("username", "student")
          .claim("AuthenticationRole", AuthenticationRole.Student.toString)
          .signWith(SignatureAlgorithm.HS256, "changeme")
          .compact()

      val serviceCall = ServiceCallFactory.authenticated[NotUsed, NotUsed](AuthenticationRole.Student){
        _ => Future.successful(NotUsed)
      }(client, server.executionContext)

      serviceCall.handleRequestHeader(addJwtsHeader(jws)).invoke().map{ answer =>
        answer should ===(NotUsed)
      }
    }

    "detect that a jwts is expired" in {
      val now = Calendar.getInstance()
      now.add(Calendar.MINUTE, -10)
      val jws =
        Jwts.builder()
          .setSubject("authentication")
          .setExpiration(now.getTime)
          .claim("username", "student")
          .claim("AuthenticationRole", AuthenticationRole.Student.toString)
          .signWith(SignatureAlgorithm.HS256, "changeme")
          .compact()

      client.check(jws).invoke().failed.map{ answer =>
        answer.asInstanceOf[TransportException].errorCode.http should ===(401)
      }
    }
  }
}
