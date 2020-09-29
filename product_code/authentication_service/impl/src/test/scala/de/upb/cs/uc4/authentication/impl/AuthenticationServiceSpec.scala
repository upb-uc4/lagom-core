package de.upb.cs.uc4.authentication.impl

import java.util.{ Base64, Calendar, Date }

import akka.cluster.sharding.typed.scaladsl.EntityRef
import akka.util.Timeout
import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.transport.{ RequestHeader, ResponseHeader }
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.{ ProducerStub, ProducerStubFactory, ServiceTest }
import com.typesafe.config.Config
import de.upb.cs.uc4.authentication.api.AuthenticationService
import de.upb.cs.uc4.authentication.impl.actor.AuthenticationState
import de.upb.cs.uc4.authentication.impl.commands.{ AuthenticationCommand, DeleteAuthentication }
import de.upb.cs.uc4.authentication.model.{ AuthenticationRole, AuthenticationUser, JsonUsername, Tokens }
import de.upb.cs.uc4.shared.client.exceptions.{ UC4Exception, ErrorType }
import de.upb.cs.uc4.shared.server.messages.Confirmation
import de.upb.cs.uc4.shared.server.{ Hashing, ServiceCallFactory }
import de.upb.cs.uc4.user.UserServiceStub
import de.upb.cs.uc4.user.api.UserService
import io.jsonwebtoken.{ Jwts, SignatureAlgorithm }
import org.scalatest.concurrent.{ Eventually, ScalaFutures }
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{ Seconds, Span }
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatest.{ Assertion, BeforeAndAfterAll }

import scala.concurrent.Future
import scala.concurrent.duration._

/** Tests for the AuthenticationService
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

    val beforeDate = Calendar.getInstance()
    beforeDate.setTime(expectedDate)
    beforeDate.add(Calendar.MINUTE, -1)

    date.before(afterDate.getTime) && date.after(beforeDate.getTime) shouldBe true
  }

  private def entityRef(id: String): EntityRef[AuthenticationCommand] =
    server.application.clusterSharding.entityRefFor(AuthenticationState.typeKey, id)

  private val hashedDefaultUsernames = Seq(Hashing.sha256("student"), Hashing.sha256("lecturer"), Hashing.sha256("admin"))

  def prepare(userToBeAdded: String): Future[Assertion] = {
    client.setAuthentication().invoke(AuthenticationUser(userToBeAdded, userToBeAdded, AuthenticationRole.Student)).flatMap {
      _ =>
        eventually(timeout(Span(15, Seconds))) {
          for {
            hashedUsernames <- server.application.database.getAll
          } yield {
            val expectedUsernames = hashedDefaultUsernames ++ Seq(Hashing.sha256(userToBeAdded))
            hashedUsernames should contain theSameElementsAs expectedUsernames
          }
        }
    }
  }

  def resetTable(usersToBeDeleted: Seq[String]): Future[Assertion] = {
    Future.sequence(usersToBeDeleted.map { user =>
      entityRef(Hashing.sha256(user)).ask[Confirmation](replyTo => DeleteAuthentication(replyTo))(Timeout(5.seconds))
    }).flatMap { _ =>
      eventually(timeout(Span(20, Seconds))) {
        for {
          hashedUsernames <- server.application.database.getAll
        } yield {
          hashedUsernames should contain theSameElementsAs hashedDefaultUsernames
        }
      }
    }
  }

  def cleanupOnFailure(usersToBeDeleted: Seq[String]): PartialFunction[Throwable, Future[Assertion]] = PartialFunction.fromFunction { throwable =>
    resetTable(usersToBeDeleted)
      .map { _ =>
        throw throwable
      }
  }

  def cleanupOnSuccess(usersToBeDeleted: Seq[String], assertion: Assertion): Future[Assertion] = {
    resetTable(usersToBeDeleted)
      .map { _ =>
        assertion
      }
  }

  /** Tests only working if the whole instance is started */
  "AuthenticationService service" should {

    "has the default login data" in {
      eventually(timeout(Span(20, Seconds))) {
        val futureAnswers = for {
          answer1 <- client.login.handleRequestHeader(addLoginHeader("student", "student")).invoke()
          answer2 <- client.login.handleRequestHeader(addLoginHeader("lecturer", "lecturer")).invoke()
          answer3 <- client.login.handleRequestHeader(addLoginHeader("admin", "admin")).invoke()
        } yield Seq(answer1, answer2, answer3)

        futureAnswers.map(answers => answers should have size 3)
      }
    }

    //UPDATE
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

      client.changePassword("Gregor").handleRequestHeader(addTokenHeader(token))
        .invoke(AuthenticationUser("Gregor", "GregNew", AuthenticationRole.Student)).flatMap {
          _ =>
            client.login.handleRequestHeader(addLoginHeader("Gregor", "GregNew")).invoke().map { answer =>
              answer should ===(Done)
            }
        }.flatMap(assertion => cleanupOnSuccess(Seq("Gregor"), assertion))
        .recoverWith(cleanupOnFailure(Seq("Gregor")))
    }

    //DELETE
    "remove a user over the topic" in {
      prepare("Test").flatMap { _ =>
        deletionStub.send(JsonUsername("Test"))

        eventually(timeout(Span(20, Seconds))) {
          client.login.handleRequestHeader(addLoginHeader("Test", "Test")).invoke().failed.map { answer =>
            answer.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.BasicAuthorization)
          }
        }.flatMap(assertion => cleanupOnSuccess(Seq("Test"), assertion))
          .recoverWith(cleanupOnFailure(Seq("Test")))
      }
    }

    //LOGIN
    "add new login data" in {
      client.setAuthentication().invoke(AuthenticationUser("Gregor", "Greg", AuthenticationRole.Student)).flatMap {
        _ =>
          client.login.handleRequestHeader(addLoginHeader("Gregor", "Greg")).invoke().map { answer =>
            answer should ===(Done)
          }
      }.flatMap(assertion => cleanupOnSuccess(Seq("Gregor"), assertion))
        .recoverWith(cleanupOnFailure(Seq("Gregor")))
    }

    "detect a wrong username" in {
      client.login.handleRequestHeader(addLoginHeader("studenta", "student")).invoke().failed.map {
        answer => answer.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.BasicAuthorization)
      }
    }

    "detect a wrong password" in {
      client.login.handleRequestHeader(addLoginHeader("student", "studenta")).invoke().failed.map {
        answer => answer.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.BasicAuthorization)
      }
    }

    "generates two correct header tokens, which" must {

      "have the right username" in {
        client.login.handleRequestHeader(addLoginHeader("admin", "admin")).withResponseHeader.invoke().map {
          case (header: ResponseHeader, _) =>
            val (refresh, login) = getTokens(header)

            val refreshClaims = Jwts.parser().setSigningKey("changeme").parseClaimsJws(refresh).getBody
            val refreshUsername = refreshClaims.get("username", classOf[String])

            val loginClaims = Jwts.parser().setSigningKey("changeme").parseClaimsJws(login).getBody
            val loginUsername = loginClaims.get("username", classOf[String])

            (refreshUsername, loginUsername) should ===("admin", "admin")
        }
      }

      "have the right role" in {
        client.login.handleRequestHeader(addLoginHeader("admin", "admin")).withResponseHeader.invoke().map {
          case (header: ResponseHeader, _) =>
            val (refresh, login) = getTokens(header)

            val refreshClaims = Jwts.parser().setSigningKey("changeme").parseClaimsJws(refresh).getBody
            val refreshAuthenticationRole = refreshClaims.get("authenticationRole", classOf[String])

            val loginClaims = Jwts.parser().setSigningKey("changeme").parseClaimsJws(login).getBody
            val loginAuthenticationRole = loginClaims.get("authenticationRole", classOf[String])

            (refreshAuthenticationRole, loginAuthenticationRole) should ===("Admin", "Admin")
        }
      }

      "have the right ExpirationDate in the refresh token" in {
        client.login.handleRequestHeader(addLoginHeader("admin", "admin")).withResponseHeader.invoke().map {
          case (header: ResponseHeader, _) =>
            val (refresh, _) = getTokens(header)

            val refreshClaims = Jwts.parser().setSigningKey("changeme").parseClaimsJws(refresh).getBody
            val refreshExpirationDate = refreshClaims.getExpiration

            val refreshExpected = Calendar.getInstance()
            refreshExpected.add(Calendar.DATE, applicationConfig.getInt("uc4.authentication.refresh"))
            checkDate(refreshExpirationDate, refreshExpected.getTime)
        }
      }

      "have the right ExpirationDate in the login token" in {
        client.login.handleRequestHeader(addLoginHeader("admin", "admin")).withResponseHeader.invoke().map {
          case (header: ResponseHeader, _) =>
            val (_, login) = getTokens(header)

            val loginClaims = Jwts.parser().setSigningKey("changeme").parseClaimsJws(login).getBody
            val loginExpirationDate = loginClaims.getExpiration

            val loginExpected = Calendar.getInstance()
            loginExpected.add(Calendar.MINUTE, applicationConfig.getInt("uc4.authentication.login"))
            checkDate(loginExpirationDate, loginExpected.getTime)
        }
      }
    }

    "generates two correct machine tokens, which" must {

      "have the right username" in {
        client.loginMachineUser.handleRequestHeader(addLoginHeader("admin", "admin")).withResponseHeader.invoke().map {
          case (_: ResponseHeader, tokens: Tokens) =>
            val refresh = tokens.refresh
            val login = tokens.login

            val refreshClaims = Jwts.parser().setSigningKey("changeme").parseClaimsJws(refresh).getBody
            val refreshUsername = refreshClaims.get("username", classOf[String])

            val loginClaims = Jwts.parser().setSigningKey("changeme").parseClaimsJws(login).getBody
            val loginUsername = loginClaims.get("username", classOf[String])

            (refreshUsername, loginUsername) should ===("admin", "admin")
        }
      }

      "have the right role" in {
        client.loginMachineUser.handleRequestHeader(addLoginHeader("admin", "admin")).withResponseHeader.invoke().map {
          case (_: ResponseHeader, tokens: Tokens) =>
            val refresh = tokens.refresh
            val login = tokens.login

            val refreshClaims = Jwts.parser().setSigningKey("changeme").parseClaimsJws(refresh).getBody
            val refreshAuthenticationRole = refreshClaims.get("authenticationRole", classOf[String])

            val loginClaims = Jwts.parser().setSigningKey("changeme").parseClaimsJws(login).getBody
            val loginAuthenticationRole = loginClaims.get("authenticationRole", classOf[String])

            (refreshAuthenticationRole, loginAuthenticationRole) should ===("Admin", "Admin")
        }
      }

      "have the right ExpirationDate in the refresh token" in {
        client.loginMachineUser.handleRequestHeader(addLoginHeader("admin", "admin")).withResponseHeader.invoke().map {
          case (_: ResponseHeader, tokens: Tokens) =>
            val refresh = tokens.refresh

            val refreshClaims = Jwts.parser().setSigningKey("changeme").parseClaimsJws(refresh).getBody
            val refreshExpirationDate = refreshClaims.getExpiration

            val refreshExpected = Calendar.getInstance()
            refreshExpected.add(Calendar.DATE, applicationConfig.getInt("uc4.authentication.refresh"))
            checkDate(refreshExpirationDate, refreshExpected.getTime)
        }
      }

      "have the right ExpirationDate in the login token" in {
        client.loginMachineUser.handleRequestHeader(addLoginHeader("admin", "admin")).withResponseHeader.invoke().map {
          case (_: ResponseHeader, tokens: Tokens) =>
            val login = tokens.login

            val loginClaims = Jwts.parser().setSigningKey("changeme").parseClaimsJws(login).getBody
            val loginExpirationDate = loginClaims.getExpiration

            val loginExpected = Calendar.getInstance()
            loginExpected.add(Calendar.MINUTE, applicationConfig.getInt("uc4.authentication.login"))
            checkDate(loginExpirationDate, loginExpected.getTime)
        }
      }
    }

    //REFRESH
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
        answer.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.RefreshTokenExpired)
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
        answer.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.RefreshTokenExpired)
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
        answer.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.MalformedRefreshToken)
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
        answer.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.MalformedRefreshToken)
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
        answer.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.RefreshTokenSignatureInvalid)
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
        answer.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.RefreshTokenSignatureInvalid)
      }
    }

    "detect that a header refresh token is missing" in {
      client.refresh.invoke().failed.map { answer =>
        answer.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.RefreshTokenMissing)
      }
    }

    "detect that a machine user refresh token is missing" in {
      client.refreshMachineUser.invoke().failed.map { answer =>
        answer.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.RefreshTokenMissing)
      }
    }

    //LOGOUT
    "log a user out, which" must {

      "set the right amount of cookies" in {
        client.logout.withResponseHeader.invoke().map {
          case (header: ResponseHeader, _) =>
            val cookies = header.getHeaders("Set-Cookie")
            cookies should have size 2
        }
      }

      "clears the refresh cookie" in {
        client.logout.withResponseHeader.invoke().map {
          case (header: ResponseHeader, _) =>
            val cookies = header.getHeaders("Set-Cookie")
            exactly(1, cookies) should include("refresh=;")
        }
      }

      "clears the login cookie" in {
        client.logout.withResponseHeader.invoke().map {
          case (header: ResponseHeader, _) =>
            val cookies = header.getHeaders("Set-Cookie")
            exactly(1, cookies) should include("login=;")
        }
      }
    }

    //SERVICE CALL FACTORY
    "detect that a user is missing privileges" in {
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

      val thrown = the[UC4Exception] thrownBy serviceCall.handleRequestHeader(addTokenHeader(token)).invoke()
      thrown.possibleErrorResponse.`type` should ===(ErrorType.NotEnoughPrivileges)
    }

    "detect that a user is authorized" in {
      val result = "Successful"
      val serviceCall = ServiceCallFactory.authenticated[NotUsed, String](AuthenticationRole.Student) {
        _ => Future.successful(result)
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
        answer should ===(result)
      }
    }

  }
}

class UserServiceStubWithTopic(deletionStub: ProducerStub[JsonUsername]) extends UserServiceStub {

  override def userDeletedTopic(): Topic[JsonUsername] = deletionStub.topic

}
