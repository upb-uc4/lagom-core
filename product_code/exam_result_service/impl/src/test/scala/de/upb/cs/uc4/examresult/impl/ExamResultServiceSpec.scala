package de.upb.cs.uc4.examresult.impl

import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import de.upb.cs.uc4.exam.api.ExamService
import de.upb.cs.uc4.examresult.impl.actor.ExamResultBehaviour
import de.upb.cs.uc4.hyperledger.api.model.JsonHyperledgerVersion
import de.upb.cs.uc4.operation.OperationServiceStub
import de.upb.cs.uc4.shared.client._
import de.upb.cs.uc4.shared.server.UC4SpecUtils
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

import scala.language.reflectiveCalls

/** Tests for the ExamResultService */
class ExamResultServiceSpec extends AsyncWordSpec
  with UC4SpecUtils with Matchers with BeforeAndAfterAll {

  private val server = ServiceTest.startServer(
    ServiceTest.defaultSetup.withCluster()
  ) { ctx =>
      new ExamResultApplication(ctx) with LocalServiceLocator {
        override lazy val examService: ExamService = null //TODO
        override lazy val operationService: OperationServiceStub = new OperationServiceStub

        var jsonStringList: Seq[String] = List()

        override def createHyperledgerActor: ExamResultBehaviour = new ExamResultBehaviour(config) {

        }
      }
    }

  val client: ExamService = server.serviceClient.implement[ExamService]
  val operation: OperationServiceStub = server.application.operationService

  override protected def afterAll(): Unit = server.stop()

  def cleanup(): Unit = {
    server.application.jsonStringList = List()
  }

  "ExamResultService service" should {

    "fetch the hyperledger versions" in {
      client.getHlfVersions.invoke().map { answer =>
        answer shouldBe a[JsonHyperledgerVersion]
      }
    }
  }
}
