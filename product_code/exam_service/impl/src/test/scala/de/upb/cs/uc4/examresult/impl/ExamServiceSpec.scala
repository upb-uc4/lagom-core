package de.upb.cs.uc4.examresult.impl

import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import de.upb.cs.uc4.course.CourseServiceStub
import de.upb.cs.uc4.exam.api.ExamService
import de.upb.cs.uc4.exam.impl.ExamApplication
import de.upb.cs.uc4.exam.impl.actor.ExamBehaviour
import de.upb.cs.uc4.examreg.DefaultTestExamRegs
import de.upb.cs.uc4.operation.OperationServiceStub
import de.upb.cs.uc4.shared.client._
import de.upb.cs.uc4.shared.server.UC4SpecUtils
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

import scala.language.reflectiveCalls

/** Tests for the MatriculationService */
class ExamServiceSpec extends AsyncWordSpec
  with UC4SpecUtils with Matchers with BeforeAndAfterAll with DefaultTestExamRegs {

  private val server = ServiceTest.startServer(
    ServiceTest.defaultSetup.withCluster()
  ) { ctx =>
      new ExamApplication(ctx) with LocalServiceLocator {
        override lazy val courseService: CourseServiceStub = new CourseServiceStub
        override lazy val operationService: OperationServiceStub = new OperationServiceStub

        courseService.resetToDefaults()

        var jsonStringList: Seq[String] = List()

        override def createActorFactory: ExamBehaviour = new ExamBehaviour(config) {

        }
      }
    }

  val client: ExamService = server.serviceClient.implement[ExamService]
  val operation: OperationServiceStub = server.application.operationService

  override protected def afterAll(): Unit = server.stop()

  def cleanup(): Unit = {
    server.application.jsonStringList = List()
  }

  "ExamService service" should {

    "fetch the hyperledger versions" in {
      client.getHlfVersions.invoke().map { answer =>
        answer shouldBe a[JsonHyperledgerVersion]
      }
    }
  }
}
