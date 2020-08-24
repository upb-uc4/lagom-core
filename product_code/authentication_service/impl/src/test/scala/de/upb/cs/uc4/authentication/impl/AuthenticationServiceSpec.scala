package de.upb.cs.uc4.authentication.impl

import java.util.Base64

import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.transport.RequestHeader
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.{ ProducerStub, ProducerStubFactory, ServiceTest }
import de.upb.cs.uc4.authentication.api.AuthenticationService
import de.upb.cs.uc4.authentication.model.{ AuthenticationRole, AuthenticationUser }
import de.upb.cs.uc4.shared.client.exceptions.CustomException
import de.upb.cs.uc4.shared.server.ServiceCallFactory
import de.upb.cs.uc4.user.api.UserService
import de.upb.cs.uc4.user.model.JsonUsername
import de.upb.cs.uc4.user.test.UserServiceStub
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

        // Create a userService with ProducerStub as topic
        override lazy val userService: UserServiceStubWithTopic = new UserServiceStubWithTopic(internDeletionStub)
      }
    }

  private val client: AuthenticationService = server.serviceClient.implement[AuthenticationService]

  override protected def afterAll(): Unit = server.stop()

  def addLoginHeader(user: String, pw: String): RequestHeader => RequestHeader = { header =>
    header.withHeader("Authorization", "Basic " + Base64.getEncoder.encodeToString(s"$user:$pw".getBytes()))
  }

  /** Tests only working if the whole instance is started */
  "AuthenticationService service" should {

    "has the default login data" in {
      eventually(timeout(Span(2, Minutes))) {
        val futureAnswers = for {
          answer1 <- client.check("student", "student").invoke()
          answer2 <- client.check("lecturer", "lecturer").invoke()
          answer3 <- client.check("admin", "admin").invoke()
        } yield Seq(answer1, answer2, answer3)

        futureAnswers.map { answers =>
          answers should contain allOf (
            ("student", AuthenticationRole.Student),
            ("lecturer", AuthenticationRole.Lecturer),
            ("admin", AuthenticationRole.Admin)
          )
        }
      }
    }

    "add new login data" in {
      client.setAuthentication().invoke(AuthenticationUser("Gregor", "Greg", AuthenticationRole.Student)).flatMap {
        _ =>
          client.check("Gregor", "Greg").invoke().map { answer =>
            answer should ===(("Gregor", AuthenticationRole.Student))
          }
      }
    }

    "update login data" in {
      client.changePassword("Gregor").handleRequestHeader(addLoginHeader("Gregor", "Greg")).invoke(AuthenticationUser("Gregor", "GregNew", AuthenticationRole.Student)).flatMap {
        _ =>
          client.check("Gregor", "GregNew").invoke().map { answer =>
            answer should ===(("Gregor", AuthenticationRole.Student))
          }
      }
    }

    "detect a wrong username" in {
      client.check("studenta", "student").invoke().failed.map {
        answer => answer.asInstanceOf[CustomException].getErrorCode.http should ===(401)
      }
    }

    "detect a wrong password" in {
      client.check("student", "studenta").invoke().failed.map {
        answer => answer.asInstanceOf[CustomException].getErrorCode.http should ===(401)
      }
    }

    "detect that a user is not authorized" in {
      val serviceCall = ServiceCallFactory.authenticated[NotUsed, NotUsed](AuthenticationRole.Admin) {
        _ => Future.successful(NotUsed)
      }(client, server.executionContext)

      serviceCall.handleRequestHeader(addLoginHeader("student", "student")).invoke().failed.map { answer =>
        answer.asInstanceOf[CustomException].getErrorCode.http should ===(403)
      }
    }

    "detect that a user is authorized" in {
      val serviceCall = ServiceCallFactory.authenticated[NotUsed, NotUsed](AuthenticationRole.Student) {
        _ => Future.successful(NotUsed)
      }(client, server.executionContext)

      serviceCall.handleRequestHeader(addLoginHeader("student", "student")).invoke().map { answer =>
        answer should ===(NotUsed)
      }
    }

    "remove a user over the topic" in {
      deletionStub.send(JsonUsername("student"))

      eventually(timeout(Span(2, Minutes))) {
        client.check("student", "student").invoke().failed.map { answer =>
          answer.asInstanceOf[CustomException].getErrorCode.http should ===(401)
        }
      }
    }

  }
}

class UserServiceStubWithTopic(deletionStub: ProducerStub[JsonUsername]) extends UserServiceStub {

  override def userDeletedTopic(): Topic[JsonUsername] = deletionStub.topic

}
