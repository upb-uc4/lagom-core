package de.upb.cs.uc4.report.impl

import akka.cluster.sharding.typed.scaladsl.EntityRef
import akka.util.{ ByteString, Timeout }
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.{ ProducerStub, ProducerStubFactory, ServiceTest, TestTopicComponents }
import de.upb.cs.uc4.certificate.CertificateServiceStub
import de.upb.cs.uc4.course.CourseServiceStub
import de.upb.cs.uc4.matriculation.MatriculationServiceStub
import de.upb.cs.uc4.report.api.ReportService
import de.upb.cs.uc4.report.impl.actor.ReportState
import de.upb.cs.uc4.report.impl.commands.{ DeleteReport, ReportCommand }
import de.upb.cs.uc4.shared.client.exceptions.{ ErrorType, UC4Exception }
import de.upb.cs.uc4.shared.client.kafka.EncryptionContainer
import de.upb.cs.uc4.shared.server.UC4SpecUtils
import de.upb.cs.uc4.shared.server.messages.Accepted
import de.upb.cs.uc4.user.api.UserService
import de.upb.cs.uc4.user.{ DefaultTestUsers, UserServiceStub }
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.Waiters.timeout
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{ Seconds, Span }
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatest.{ Assertion, BeforeAndAfterAll, BeforeAndAfterEach }

import scala.concurrent.Future
import scala.concurrent.duration._

/** Tests for the ReportService */
class ReportServiceSpec extends AsyncWordSpec
  with UC4SpecUtils with Matchers with BeforeAndAfterAll with BeforeAndAfterEach with DefaultTestUsers {

  private var deletionStub: ProducerStub[EncryptionContainer] = _ //EncryptionContainer[JsonUsername]

  private val server = ServiceTest.startServer(
    ServiceTest.defaultSetup.withJdbc()
  ) { ctx =>
      new ReportApplication(ctx) with LocalServiceLocator {
        override lazy val userService: UserServiceStub = new UserServiceStub
        override lazy val certificateService: CertificateServiceStub = new CertificateServiceStub
        override lazy val courseService: CourseServiceStub = new CourseServiceStub
        override lazy val matriculationService: MatriculationServiceStub = new MatriculationServiceStub

        lazy val stubFactory = new ProducerStubFactory(actorSystem, materializer)
        lazy val internDeletionStub: ProducerStub[EncryptionContainer] =
          stubFactory.producer[EncryptionContainer](UserService.DELETE_TOPIC_MINIMAL_NAME)
        deletionStub = internDeletionStub
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
    certificate.setup(student0.username, lecturer0.username)
    matriculation.addImmatriculationData(
      student0.username,
      matriculation.createSingleImmatriculationData(certificate.get(student0.username).enrollmentId, "Bachelor Computer Science v3", "SS2020")
    )
  }

  private def entityRef(id: String): EntityRef[ReportCommand] =
    server.application.clusterSharding.entityRefFor(ReportState.typeKey, id)

  def prepare(usernames: String*): Future[Seq[String]] = {
    Future.sequence(usernames.map { username =>
      client.prepareUserData(username).handleRequestHeader(addAuthorizationHeader(username)).invoke()
    }).flatMap { _ =>
      eventually(timeout(Span(15, Seconds))) {
        Future.sequence(usernames.map { username =>
          client.getUserData(username).handleRequestHeader(addAuthorizationHeader(username)).invoke()
        }).map(_.length should ===(usernames.length))
      }
    }.map(_ => usernames)
  }

  def deleteAllReports(usernames: String*): Future[Assertion] = {
    eventually(timeout(Span(15, Seconds))) {
      Future.sequence(usernames.map { username =>
        entityRef(username).ask(replyTo => DeleteReport(replyTo))(Timeout(5.seconds))
      }).map(_.forall(_.isInstanceOf[Accepted]) should ===(true))
    }
  }

  def cleanupOnFailure(usernames: String*): PartialFunction[Throwable, Future[Assertion]] = PartialFunction.fromFunction { throwable =>
    deleteAllReports(usernames: _*)
      .map { _ =>
        throw throwable
      }
  }

  def cleanupOnSuccess(assertion: Assertion, usernames: String*): Future[Assertion] = {
    deleteAllReports(usernames: _*)
      .map { _ =>
        assertion
      }
  }

  "Report service" should {

    //PREPARE
    "prepare data for a student" in {
      val username = student0.username

      client.prepareUserData(username).handleRequestHeader(addAuthorizationHeader(username)).invoke().flatMap { _ =>
        client.getUserData(username).handleRequestHeader(addAuthorizationHeader(username)).invoke().map { answer =>
          answer shouldBe a[ByteString]
        }
      }.flatMap(cleanupOnSuccess(_, username))
        .recoverWith(cleanupOnFailure(username))
    }

    "prepare data for a lecturer" in {
      val username = lecturer0.username

      client.prepareUserData(username).handleRequestHeader(addAuthorizationHeader(username)).invoke().flatMap { _ =>
        client.getUserData(username).handleRequestHeader(addAuthorizationHeader(username)).invoke().map { answer =>
          answer shouldBe a[ByteString]
        }
      }.flatMap(cleanupOnSuccess(_, username))
        .recoverWith(cleanupOnFailure(username))
    }

    "throw an error when trying to prepare a report for another user" in {
      client.prepareUserData(student0.username).handleRequestHeader(addAuthorizationHeader()).invoke().failed.map { exception =>
        exception.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.OwnerMismatch)
      }
    }

    //GET
    "throw an error when trying to get the report of another user" in {
      client.getUserData(student0.username).handleRequestHeader(addAuthorizationHeader()).invoke().failed.map { exception =>
        exception.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.OwnerMismatch)
      }
    }

    "get the report of a user" in {
      val username = admin0.username

      prepare(username).flatMap { _ =>
        client.getUserData(username).handleRequestHeader(addAuthorizationHeader(username)).invoke().map { zippedReport =>
          zippedReport shouldBe a[ByteString]
          //TODO : test if the zipped file is actually correct
        }
      }.flatMap(cleanupOnSuccess(_, username))
        .recoverWith(cleanupOnFailure(username))
    }
  }
}
