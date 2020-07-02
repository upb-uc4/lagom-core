package de.upb.cs.uc4.authentication.impl

import java.util.Base64

import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.transport.{RequestHeader, TransportException}
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import de.upb.cs.uc4.authentication.api.AuthenticationService
import de.upb.cs.uc4.authentication.model.AuthenticationRole
import de.upb.cs.uc4.authentication.model.AuthenticationRole.AuthenticationRole
import de.upb.cs.uc4.shared.ServiceCallFactory
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

  /** Tests only working if the whole instance is started */
  "AuthenticationService service" should {

    "have the standard admin" in {
      client.check("admin", "admin").invoke().map{ answer =>
        answer shouldBe a [(String, AuthenticationRole)]
      }
    }

    "have the standard lecturer" in {
      client.check("lecturer", "lecturer").invoke().map{ answer =>
        answer shouldBe a [(String, AuthenticationRole)]
      }
    }

    "have the standard student" in {
      client.check("student", "student").invoke().map{ answer =>
        answer shouldBe a [(String, AuthenticationRole)]
      }
    }

    "detect a wrong username" in {
      client.check("studenta", "student").invoke().failed.map{
        answer => answer.asInstanceOf[TransportException].errorCode.http should ===(401)
      }
    }

    "detect a wrong password" in {
      client.check("student", "studenta").invoke().failed.map{
        answer => answer.asInstanceOf[TransportException].errorCode.http should ===(401)
      }
    }

    "detect that a user is not authorized" in {
      val serviceCall = ServiceCallFactory.authenticated[NotUsed, NotUsed](AuthenticationRole.Admin){
         _ => Future.successful(NotUsed)
      }(client, server.executionContext)

      serviceCall.handleRequestHeader(addLoginHeader("student", "student")).invoke().failed.map{ answer =>
        answer.asInstanceOf[TransportException].errorCode.http should ===(403)
      }
    }

    "detect that a user is authorized" in {
      val serviceCall = ServiceCallFactory.authenticated[NotUsed, NotUsed](AuthenticationRole.Student){
        _ => Future.successful(NotUsed)
      }(client, server.executionContext)

      serviceCall.handleRequestHeader(addLoginHeader("student", "student")).invoke().map{ answer =>
        answer should ===(NotUsed)
      }
    }
  }
}
