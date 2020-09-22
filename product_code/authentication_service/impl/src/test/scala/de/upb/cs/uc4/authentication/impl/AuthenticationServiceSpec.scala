package de.upb.cs.uc4.authentication.impl

import java.util.{ Base64, Calendar, Date }

import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.transport.{ RequestHeader, ResponseHeader }
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.{ ProducerStub, ProducerStubFactory, ServiceTest }
import com.typesafe.config.Config
import de.upb.cs.uc4.authentication.api.AuthenticationService
import de.upb.cs.uc4.authentication.model.{ AuthenticationRole, AuthenticationUser, JsonUsername, Tokens }
import de.upb.cs.uc4.shared.client.exceptions.CustomException
import de.upb.cs.uc4.shared.server.ServiceCallFactory
import de.upb.cs.uc4.user.UserServiceStub
import de.upb.cs.uc4.user.api.UserService
import io.jsonwebtoken.{ Jwts, SignatureAlgorithm }
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.{ Eventually, ScalaFutures }
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{ Minutes, Span }
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

  def addTokenHeader(token: String, cookie: String = "login"): RequestHeader => RequestHeader = { header =>
    header.withHeader("Cookie", s"$cookie=$token")
  }

  def addBearerToken(token: String): RequestHeader => RequestHeader = { header =>
    header.withHeader("Authorization", s"Bearer $token")
  }

  private def getTokens(responseHeader: ResponseHeader): (String, String) = {
    responseHeader.getHeaders("Set-Cookie") match {
      case Seq(
        s"refresh=$refresh;$_",
        s"login=$login;$_"
        ) => (refresh, login)
    }
  }

  private def checkDate(date: Date, expectedDate: Date) = {
    val afterDate = Calendar.getInstance()
    afterDate.setTime(expectedDate)
    afterDate.add(Calendar.MINUTE, 1)

    date.before(afterDate.getTime) shouldBe true

    val beforeDate = Calendar.getInstance()
    beforeDate.setTime(expectedDate)
    beforeDate.add(Calendar.MINUTE, -1)

    date.after(beforeDate.getTime) shouldBe true
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

    "generates two correct header tokens" in {
      client.login.handleRequestHeader(addLoginHeader("admin", "admin")).withResponseHeader.invoke().map {
        case (header: ResponseHeader, _) =>
          val (refresh, login) = getTokens(header)

          val refreshClaims = Jwts.parser().setSigningKey("changeme").parseClaimsJws(refresh).getBody
          val refreshUsername = refreshClaims.get("username", classOf[String])
          val refreshAuthenticationRole = refreshClaims.get("authenticationRole", classOf[String])
          val refreshExpirationDate = refreshClaims.getExpiration

          val loginClaims = Jwts.parser().setSigningKey("changeme").parseClaimsJws(login).getBody
          val loginUsername = loginClaims.get("username", classOf[String])
          val loginAuthenticationRole = loginClaims.get("authenticationRole", classOf[String])
          val loginExpirationDate = loginClaims.getExpiration

          refreshUsername should ===("admin")
          refreshAuthenticationRole should ===("Admin")
          loginUsername should ===("admin")
          loginAuthenticationRole should ===("Admin")

          val refreshExpected = Calendar.getInstance()
          refreshExpected.add(Calendar.DATE, applicationConfig.getInt("uc4.authentication.refresh"))
          checkDate(refreshExpirationDate, refreshExpected.getTime)

          val loginExpected = Calendar.getInstance()
          loginExpected.add(Calendar.MINUTE, applicationConfig.getInt("uc4.authentication.login"))
          checkDate(loginExpirationDate, loginExpected.getTime)
      }
    }

    "generates two correct machine tokens" in {
      client.loginMachineUser.handleRequestHeader(addLoginHeader("admin", "admin")).withResponseHeader.invoke().map {
        case (header: ResponseHeader, tokens: Tokens) =>
          val refresh = tokens.refresh
          val login = tokens.login

          val refreshClaims = Jwts.parser().setSigningKey("changeme").parseClaimsJws(refresh).getBody
          val refreshUsername = refreshClaims.get("username", classOf[String])
          val refreshAuthenticationRole = refreshClaims.get("authenticationRole", classOf[String])
          val refreshExpirationDate = refreshClaims.getExpiration

          val loginClaims = Jwts.parser().setSigningKey("changeme").parseClaimsJws(login).getBody
          val loginUsername = loginClaims.get("username", classOf[String])
          val loginAuthenticationRole = loginClaims.get("authenticationRole", classOf[String])
          val loginExpirationDate = loginClaims.getExpiration

          refreshUsername should ===("admin")
          refreshAuthenticationRole should ===("Admin")
          loginUsername should ===("admin")
          loginAuthenticationRole should ===("Admin")

          val refreshExpected = Calendar.getInstance()
          refreshExpected.add(Calendar.DATE, applicationConfig.getInt("uc4.authentication.refresh"))
          checkDate(refreshExpirationDate, refreshExpected.getTime)

          val loginExpected = Calendar.getInstance()
          loginExpected.add(Calendar.MINUTE, applicationConfig.getInt("uc4.authentication.login"))
          checkDate(loginExpirationDate, loginExpected.getTime)
      }
    }

    "refresh a header login token" in {
      val time = Calendar.getInstance()
      time.add(Calendar.DATE, 1)

      val token =
        Jwts.builder()
          .setSubject("refresh")
          .setExpiration(time.getTime)
          .claim("username", "daniel")
          .claim("authenticationRole", "Student")
          .signWith(SignatureAlgorithm.HS256, "changeme")
          .compact()

      client.refresh.handleRequestHeader(addTokenHeader(token, "refresh")).invoke().map { answer =>
        answer.username should ===("daniel")
      }
    }

    "refresh a machine login token" in {
      val time = Calendar.getInstance()
      time.add(Calendar.DATE, 1)

      val token =
        Jwts.builder()
          .setSubject("refresh")
          .setExpiration(time.getTime)
          .claim("username", "daniel")
          .claim("authenticationRole", "Student")
          .signWith(SignatureAlgorithm.HS256, "changeme")
          .compact()

      client.refreshMachineUser.handleRequestHeader(addBearerToken(token)).invoke().map { answer =>
        answer.username should ===("daniel")
      }
    }

    "detect that a header refresh token is expired" in {
      val time = Calendar.getInstance()
      time.add(Calendar.DATE, -1)

      val token =
        Jwts.builder()
          .setSubject("refresh")
          .setExpiration(time.getTime)
          .claim("username", "lars")
          .claim("authenticationRole", "Student")
          .signWith(SignatureAlgorithm.HS256, "changeme")
          .compact()

      client.refresh.handleRequestHeader(addTokenHeader(token, "refresh")).invoke().failed.map { answer =>
        answer.asInstanceOf[CustomException].getErrorCode.http should ===(401)
      }
    }

    "detect that a machine refresh token is expired" in {
      val time = Calendar.getInstance()
      time.add(Calendar.DATE, -1)

      val token =
        Jwts.builder()
          .setSubject("refresh")
          .setExpiration(time.getTime)
          .claim("username", "lars")
          .claim("authenticationRole", "Student")
          .signWith(SignatureAlgorithm.HS256, "changeme")
          .compact()

      client.refreshMachineUser.handleRequestHeader(addBearerToken(token)).invoke().failed.map { answer =>
        answer.asInstanceOf[CustomException].getErrorCode.http should ===(401)
      }
    }

    "detect that a header refresh token is malformed" in {
      val time = Calendar.getInstance()
      time.add(Calendar.DATE, 1)

      val token =
        Jwts.builder()
          .setSubject("refresh")
          .setExpiration(time.getTime)
          .claim("username", "lars")
          .claim("authenticationRole", "Student")
          .signWith(SignatureAlgorithm.HS256, "changeme")
          .compact().replaceFirst(".", ",")

      client.refresh.handleRequestHeader(addTokenHeader(token, "refresh")).invoke().failed.map { answer =>
        answer.asInstanceOf[CustomException].getErrorCode.http should ===(400)
      }
    }

    "detect that a machine refresh token is malformed" in {
      val time = Calendar.getInstance()
      time.add(Calendar.DATE, 1)

      val token =
        Jwts.builder()
          .setSubject("refresh")
          .setExpiration(time.getTime)
          .claim("username", "lars")
          .claim("authenticationRole", "Student")
          .signWith(SignatureAlgorithm.HS256, "changeme")
          .compact().replaceFirst(".", ",")

      client.refreshMachineUser.handleRequestHeader(addBearerToken(token)).invoke().failed.map { answer =>
        answer.asInstanceOf[CustomException].getErrorCode.http should ===(400)
      }
    }

    "detect that a header refresh token has a wrong signature" in {
      val time = Calendar.getInstance()
      time.add(Calendar.DATE, 1)

      val token =
        Jwts.builder()
          .setSubject("refresh")
          .setExpiration(time.getTime)
          .claim("username", "lars")
          .claim("authenticationRole", "Student")
          .signWith(SignatureAlgorithm.HS256, "changemeagain")
          .compact()

      client.refresh.handleRequestHeader(addTokenHeader(token, "refresh")).invoke().failed.map { answer =>
        answer.asInstanceOf[CustomException].getErrorCode.http should ===(422)
      }
    }

    "detect that a machine refresh token has a wrong signature" in {
      val time = Calendar.getInstance()
      time.add(Calendar.DATE, 1)

      val token =
        Jwts.builder()
          .setSubject("refresh")
          .setExpiration(time.getTime)
          .claim("username", "lars")
          .claim("authenticationRole", "Student")
          .signWith(SignatureAlgorithm.HS256, "changemeagain")
          .compact()

      client.refreshMachineUser.handleRequestHeader(addBearerToken(token)).invoke().failed.map { answer =>
        answer.asInstanceOf[CustomException].getErrorCode.http should ===(422)
      }
    }

    "detect that a header refresh token is missing" in {
      client.refresh.invoke().failed.map { answer =>
        answer.asInstanceOf[CustomException].getErrorCode.http should ===(401)
      }
    }

    "detect that a machine refresh token is missing" in {
      client.refreshMachineUser.invoke().failed.map { answer =>
        answer.asInstanceOf[CustomException].getErrorCode.http should ===(401)
      }
    }

    "log a user out" in {
      client.logout.withResponseHeader.invoke().map {
        case (header: ResponseHeader, _) =>
          val cookies = header.getHeaders("Set-Cookie")
          cookies should have size 2
          exactly(1, cookies) should include("refresh=;")
          exactly(1, cookies) should include("login=;")
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

      val thrown = the[CustomException] thrownBy serviceCall.handleRequestHeader(addTokenHeader(token)).invoke()
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
