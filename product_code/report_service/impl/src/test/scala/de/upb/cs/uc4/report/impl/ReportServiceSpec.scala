package de.upb.cs.uc4.report.impl

import akka.Done
import akka.cluster.sharding.typed.scaladsl.EntityRef
import akka.util.ByteString
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.{ ProducerStub, ProducerStubFactory, ServiceTest }
import de.upb.cs.uc4.certificate.CertificateServiceStub
import de.upb.cs.uc4.course.CourseServiceStub
import de.upb.cs.uc4.matriculation.MatriculationServiceStub
import de.upb.cs.uc4.report.api.ReportService
import de.upb.cs.uc4.report.impl.actor.ReportState
import de.upb.cs.uc4.report.impl.commands.ReportCommand
import de.upb.cs.uc4.shared.client.exceptions.{ ErrorType, UC4Exception }
import de.upb.cs.uc4.shared.client.kafka.EncryptionContainer
import de.upb.cs.uc4.shared.server.UC4SpecUtils
import de.upb.cs.uc4.user.api.UserService
import de.upb.cs.uc4.user.{ DefaultTestUsers, UserServiceStub }
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.Waiters.timeout
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{ Seconds, Span }
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatest.{ Assertion, BeforeAndAfterAll, BeforeAndAfterEach }

import scala.concurrent.Future

/** Tests for the ReportService */
class ReportServiceSpec extends AsyncWordSpec
  with UC4SpecUtils with Matchers with BeforeAndAfterAll with BeforeAndAfterEach with DefaultTestUsers {

  private var deletionStub: ProducerStub[EncryptionContainer] = _ //EncryptionContainer[JsonUsername]

  private val server = ServiceTest.startServer(
    ServiceTest.defaultSetup.withJdbc()
  ) { ctx =>
      new ReportApplication(ctx) with LocalServiceLocator {
        lazy val stubFactory = new ProducerStubFactory(actorSystem, materializer)
        lazy val internDeletionStub: ProducerStub[EncryptionContainer] =
          stubFactory.producer[EncryptionContainer](UserService.DELETE_TOPIC_MINIMAL_NAME)
        deletionStub = internDeletionStub

        override lazy val certificateService: CertificateServiceStub = new CertificateServiceStub
        override lazy val courseService: CourseServiceStub = new CourseServiceStub
        override lazy val matriculationService: MatriculationServiceStub = new MatriculationServiceStub
        override lazy val userService: UserServiceStub = new UserServiceStubWithTopic(internDeletionStub)
      }
    }

  val client: ReportService = server.serviceClient.implement[ReportService]
  val certificate: CertificateServiceStub = server.application.certificateService
  val user: UserServiceStub = server.application.userService
  val course: CourseServiceStub = server.application.courseService
  val matriculation: MatriculationServiceStub = server.application.matriculationService

  override protected def afterAll(): Unit = server.stop()

  override protected def beforeAll(): Unit = {
    user.resetToDefaults() //student/lecturer/admin 0-2
    course.resetToDefaults() //Courses for lecturer0 and 1
    certificate.setup(student0.username, lecturer0.username, admin0.username)
    matriculation.addImmatriculationData(
      student0.username,
      matriculation.createSingleImmatriculationData(certificate.get(student0.username).enrollmentId, "Bachelor Computer Science v3", "SS2020")
    )
  }

  private def entityRef(id: String): EntityRef[ReportCommand] =
    server.application.clusterSharding.entityRefFor(ReportState.typeKey, id)

  def cleanupOnFailure(usernames: String*): PartialFunction[Throwable, Future[Assertion]] = PartialFunction.fromFunction { throwable =>
    Future.sequence(usernames.map {
      username =>
        client.deleteUserReport(username).handleRequestHeader(addAuthorizationHeader(username)).invoke()
    }).map(_ => throw throwable)
  }

  def cleanupOnSuccess(assertion: Assertion, usernames: String*): Future[Assertion] = {
    Future.sequence(usernames.map {
      username =>
        client.deleteUserReport(username).handleRequestHeader(addAuthorizationHeader(username)).invoke()
    }).map { _ =>
      assertion
    }
  }

  "Report service" should {

    // GET
    "start the preparing process for a student" in {
      val username = student0.username

      client.getUserReport(username).handleRequestHeader(addAuthorizationHeader(username)).invoke().map { answer =>
        answer should ===(ByteString.empty)
      }.flatMap(cleanupOnSuccess(_, username))
        .recoverWith(cleanupOnFailure(username))
    }
    "fetch a prepared report if the report was prepared beforehand" in {
      val username = student0.username

      client.getUserReport(username).handleRequestHeader(addAuthorizationHeader(username)).invoke().flatMap { _ =>
        eventually(timeout(Span(15, Seconds))) {
          client.getUserReport(username).handleRequestHeader(addAuthorizationHeader(username)).invoke().map { byteString =>
            byteString should !==(ByteString.empty)
          }
        }
      }.flatMap(cleanupOnSuccess(_, username))
        .recoverWith(cleanupOnFailure(username))
    }

    "throw an error when trying to prepare a report for another user" in {
      client.getUserReport(student0.username).handleRequestHeader(addAuthorizationHeader(lecturer0.username)).invoke().failed.map { exception =>
        exception.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.OwnerMismatch)
      }
    }

    // DELETE
    "delete a non-existing report" in {
      val username = student0.username
      client.deleteUserReport(username).handleRequestHeader(addAuthorizationHeader(username)).invoke().map { answer =>
        answer should ===(Done)
      }.flatMap(cleanupOnSuccess(_, username))
        .recoverWith(cleanupOnFailure(username))
    }

    "delete a report" in {
      val username = student0.username

      client.getUserReport(username).handleRequestHeader(addAuthorizationHeader(username)).invoke().flatMap { _ =>
        client.deleteUserReport(username).handleRequestHeader(addAuthorizationHeader(username)).invoke().map { answer =>
          answer should ===(Done)
        }
      }.flatMap(cleanupOnSuccess(_, username))
        .recoverWith(cleanupOnFailure(username))
    }

    "throw an error when trying to delete the report of another user" in {
      client.deleteUserReport(student0.username).handleRequestHeader(addAuthorizationHeader(lecturer0.username)).invoke().failed.map { exception =>
        exception.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.OwnerMismatch)
      }
    }
  }
}

class UserServiceStubWithTopic(deletionStub: ProducerStub[EncryptionContainer]) extends UserServiceStub {

  override def userDeletionTopicMinimal(): Topic[EncryptionContainer] = deletionStub.topic

}

