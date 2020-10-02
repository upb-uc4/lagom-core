package de.upb.cs.uc4.shared.server

import java.io.File
import java.util.Calendar

import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.transport.RequestHeader
import com.typesafe.config._
import de.upb.cs.uc4.authentication.model.AuthenticationRole
import de.upb.cs.uc4.shared.client.exceptions.{ ErrorType, UC4Exception }
import io.jsonwebtoken.{ Jwts, SignatureAlgorithm }
import org.scalatest.PrivateMethodTester
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

import scala.concurrent.Future

class ServiceCallFactorySpec extends AsyncWordSpec with Matchers with PrivateMethodTester {

  private val applicationConfig: Config = {
    com.typesafe.config.ConfigFactory.parseFile(new File(getClass.getResource("/application.conf").getPath))
  }

  def addTokenHeader(token: String, cookie: String = "login"): RequestHeader => RequestHeader = { header =>
    header.withHeader("Cookie", s"$cookie=$token")
  }

  "ServiceCallFactory" should {

    "detect that a user is missing privileges" in {
      val serviceCall = ServiceCallFactory.authenticated[NotUsed, NotUsed](AuthenticationRole.Admin) {
        _ => Future.successful(NotUsed)
      }(applicationConfig, executionContext)

      val time = Calendar.getInstance()
      time.add(Calendar.DATE, 1)

      val token =
        Jwts.builder()
          .setSubject("login")
          .setExpiration(time.getTime)
          .claim("username", "student")
          .claim("authenticationRole", "Student")
          .signWith(SignatureAlgorithm.HS256, "Y2hhbmdlbWU=")
          .compact()

      val thrown = the[UC4Exception] thrownBy serviceCall.handleRequestHeader(addTokenHeader(token)).invoke()
      thrown.possibleErrorResponse.`type` should ===(ErrorType.NotEnoughPrivileges)
    }

    "detect that a user is authorized" in {
      val result = "Successful"
      val serviceCall = ServiceCallFactory.authenticated[NotUsed, String](AuthenticationRole.Student) {
        _ => Future.successful(result)
      }(applicationConfig, executionContext)

      val time = Calendar.getInstance()
      time.add(Calendar.DATE, 1)

      val token =
        Jwts.builder()
          .setSubject("login")
          .setExpiration(time.getTime)
          .claim("username", "student")
          .claim("authenticationRole", "Student")
          .signWith(SignatureAlgorithm.HS256, "Y2hhbmdlbWU=")
          .compact()

      serviceCall.handleRequestHeader(addTokenHeader(token)).invoke().map { answer =>
        answer should ===(result)
      }
    }
  }
}
