package de.upb.cs.uc4.report.impl

import akka.util.ByteString
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import de.upb.cs.uc4.certificate.CertificateServiceStub
import de.upb.cs.uc4.course.CourseServiceStub
import de.upb.cs.uc4.matriculation.MatriculationServiceStub
import de.upb.cs.uc4.report.api.ReportService
import de.upb.cs.uc4.shared.server.UC4SpecUtils
import de.upb.cs.uc4.user.{ DefaultTestUsers, UserServiceStub }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatest.{ BeforeAndAfterAll, BeforeAndAfterEach }

/** Tests for the MatriculationService */
class ReportServiceSpec extends AsyncWordSpec
  with UC4SpecUtils with Matchers with BeforeAndAfterAll with BeforeAndAfterEach with DefaultTestUsers {

  private val server = ServiceTest.startServer(
    ServiceTest.defaultSetup.withCluster()
  ) { ctx =>
    new ReportApplication(ctx) with LocalServiceLocator {
      override lazy val userService: UserServiceStub = new UserServiceStub
      override lazy val certificateService: CertificateServiceStub = new CertificateServiceStub
      override lazy val courseService: CourseServiceStub = new CourseServiceStub
      override lazy val matriculationService: MatriculationServiceStub = new MatriculationServiceStub
    }
  }

  val client: ReportService = server.serviceClient.implement[ReportServiceImpl]
  val certificate: CertificateServiceStub = server.application.certificateService
  val user: UserServiceStub = server.application.userService
  val course: CourseServiceStub = server.application.courseService
  val matriculation: MatriculationServiceStub = server.application.matriculationService

  override protected def afterAll(): Unit = server.stop()

  override protected def beforeEach(): Unit = {
    user.resetToDefaults()
    course.resetToDefaults()
    certificate.setup(student0.username, lecturer0.username)
    matriculation.addImmatriculationData(
      student0.username,
      matriculation.createSingleImmatriculationData(certificate.get(student0.username).enrollmentId, "Bachelor Computer Science v3", "SS2020")
    )
  }

  "Report service" should {

    //PREPARE
    "prepare data for a student" in {
      client.prepareUserData(student0.username).handleRequestHeader(addAuthorizationHeader(student0.username)).invoke().flatMap { _ =>
        client.getUserData(student0.username).handleRequestHeader(addAuthorizationHeader(student0.username)).invoke().map { answer =>
          answer shouldBe a [ByteString]
        }
      }
    }

    "prepare data for a lecturer" in {
      client.prepareUserData(lecturer0.username).handleRequestHeader(addAuthorizationHeader(lecturer0.username)).invoke().flatMap { _ =>
        client.getUserData(lecturer0.username).handleRequestHeader(addAuthorizationHeader(lecturer0.username)).invoke().map { answer =>
          answer shouldBe a [ByteString]
        }
      }
    }
  }
}

