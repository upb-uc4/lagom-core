package de.upb.cs.uc4.authentication.impl

import java.util.{Base64, Calendar}

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.transport.RequestHeader
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.{ProducerStub, ProducerStubFactory, ServiceTest}
import com.typesafe.config.Config
import de.upb.cs.uc4.authentication.api.AuthenticationService
import de.upb.cs.uc4.authentication.model.{AuthenticationRole, AuthenticationUser, JsonUsername}
import de.upb.cs.uc4.shared.client.exceptions.CustomException
import de.upb.cs.uc4.shared.server.ServiceCallFactory
import de.upb.cs.uc4.user.UserServiceStub
import de.upb.cs.uc4.user.api.UserService
import io.jsonwebtoken.{Jwts, SignatureAlgorithm}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Minutes, Span}
import org.scalatest.wordspec.AsyncWordSpec

import scala.concurrent.Future

/** Tests for the AuthenticationService
  * All tests need to be started in the defined order
  */
class AuthenticationServiceSpec extends AsyncWordSpec
  with Matchers with BeforeAndAfterAll with Eventually with ScalaFutures {

  private var deletionStub: ProducerStub[JsonUsername] = _
  private var applicationConfig: Config = _

  private val server = ServiceTest.startServer(
    ServiceTest.defaultSetup
      .withJdbc()
  ) { ctx =>
    new AuthenticationApplication(ctx) with LocalServiceLocator {
      // Declaration as lazy values forces right execution order
      lazy val stubFactory = new ProducerStubFactory(actorSystem, materializer)
      lazy val internDeletionStub: ProducerStub[JsonUsername] =
        stubFactory.producer[JsonUsername](UserService.DELETE_TOPIC_NAME)

      deletionStub = internDeletionStub
      applicationConfig = config

      // Create a userService with ProducerStub as topic
      override lazy val userService: UserServiceStubWithTopic = new UserServiceStubWithTopic(internDeletionStub)
    }
  }

  private val client: AuthenticationService = server.serviceClient.implement[AuthenticationService]

  override protected def afterAll(): Unit = server.stop()

  def addLoginHeader(user: String, pw: String): RequestHeader => RequestHeader = { header =>
    header.withHeader("Authorization", "Basic " + Base64.getEncoder.encodeToString(s"$user:$pw".getBytes()))
  }

  def addTokenHeader(token: String): RequestHeader => RequestHeader = { header =>
    header.withHeader("Cookie", s"login=$token")
  }

  /** Tests only working if the whole instance is started */
  "AuthenticationService service" should {

    "has the default login data" in {
      eventually(timeout(Span(2, Minutes))) {
        val futureAnswers = for {
          answer1 <- client.login.handleRequestHeader(addLoginHeader("student", "student")).invoke()
          answer2 <- client.login.handleRequestHeader(addLoginHeader("lecturer", "lecturer")).invoke()
          answer3 <- client.login.handleRequestHeader(addLoginHeader("admin", "admin")).invoke()
        } yield Seq(answer1, answer2, answer3)

        futureAnswers.map(answers => answers should have size 3)
      }
    }

    "add new login data" in {
      client.setAuthentication().invoke(AuthenticationUser("Gregor", "Greg", AuthenticationRole.Student)).flatMap {
        _ =>
          client.login.handleRequestHeader(addLoginHeader("Gregor", "Greg")).invoke().map { answer =>
            answer should ===(Done)
          }
      }
    }

    "update login data" in {
      val time = Calendar.getInstance()
      time.add(Calendar.DATE, 1)

      val token =
        Jwts.builder()
          .setSubject("login")
          .setExpiration(time.getTime)
          .claim("username", "Gregor")
          .claim("authenticationRole", "Student")
          .signWith(SignatureAlgorithm.HS256, "changeme")
          .compact()

      client.changePassword("Gregor").handleRequestHeader(addTokenHeader(token)).invoke(AuthenticationUser("Gregor", "GregNew", AuthenticationRole.Student)).flatMap {
        _ =>
          client.login.handleRequestHeader(addLoginHeader("Gregor", "GregNew")).invoke().map { answer =>
            answer should ===(Done)
          }
      }
    }

    "detect a wrong username" in {
      client.login.handleRequestHeader(addLoginHeader("studenta", "student")).invoke().failed.map {
        answer => answer.asInstanceOf[CustomException].getErrorCode.http should ===(401)
      }
    }

    "detect a wrong password" in {
      client.login.handleRequestHeader(addLoginHeader("student", "studenta")).invoke().failed.map {
        answer => answer.asInstanceOf[CustomException].getErrorCode.http should ===(401)
      }
    }

    "detect that a user is not authorized" in {
      val serviceCall = ServiceCallFactory.authenticated[NotUsed, NotUsed](AuthenticationRole.Admin) {
        _ => Future.successful(NotUsed)
      }(applicationConfig, server.executionContext)

      val time = Calendar.getInstance()
      time.add(Calendar.DATE, 1)

      val token =
        Jwts.builder()
          .setSubject("login")
          .setExpiration(time.getTime)
          .claim("username", "student")
          .claim("authenticationRole", "Student")
          .signWith(SignatureAlgorithm.HS256, "changeme")
          .compact()

      val thrown = the [CustomException] thrownBy serviceCall.handleRequestHeader(addTokenHeader(token)).invoke()
      thrown.getErrorCode.http should ===(403)
    }

    "detect that a user is authorized" in {
      val serviceCall = ServiceCallFactory.authenticated[NotUsed, NotUsed](AuthenticationRole.Student) {
        _ => Future.successful(NotUsed)
      }(applicationConfig, server.executionContext)

      val time = Calendar.getInstance()
      time.add(Calendar.DATE, 1)

      val token =
        Jwts.builder()
          .setSubject("login")
          .setExpiration(time.getTime)
          .claim("username", "student")
          .claim("authenticationRole", "Student")
          .signWith(SignatureAlgorithm.HS256, "changeme")
          .compact()

      serviceCall.handleRequestHeader(addTokenHeader(token)).invoke().map { answer =>
        answer should ===(NotUsed)
      }
    }

    "remove a user over the topic" in {
      deletionStub.send(JsonUsername("student"))

      eventually(timeout(Span(2, Minutes))) {
        client.login.handleRequestHeader(addLoginHeader("student", "student")).invoke().failed.map { answer =>
          answer.asInstanceOf[CustomException].getErrorCode.http should ===(401)
        }
      }
    }

  }
}

class UserServiceStubWithTopic(deletionStub: ProducerStub[JsonUsername]) extends UserServiceStub {

  override def userDeletedTopic(): Topic[JsonUsername] = deletionStub.topic

}
