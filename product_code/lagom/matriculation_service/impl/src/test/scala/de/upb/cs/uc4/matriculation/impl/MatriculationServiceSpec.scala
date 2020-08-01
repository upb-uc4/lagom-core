package de.upb.cs.uc4.matriculation.impl

import java.util.Base64

import akka.Done
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport.{RequestHeader, TransportErrorCode}
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import de.upb.cs.uc4.authentication.api.AuthenticationService
import de.upb.cs.uc4.authentication.model.AuthenticationRole
import de.upb.cs.uc4.hyperledger.api.HyperLedgerService
import de.upb.cs.uc4.matriculation.api.MatriculationService
import de.upb.cs.uc4.shared.client.exceptions.{CustomException, DetailedError}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

import scala.concurrent.Future

/** Tests for the MatriculationService
 *
 * All tests need to be started in the defined order
 */
class MatriculationServiceSpec extends AsyncWordSpec with Matchers with BeforeAndAfterAll {

  private val server = ServiceTest.startServer(
    ServiceTest.defaultSetup
  ) { ctx =>
    new MatriculationApplication(ctx) with LocalServiceLocator {
      override lazy val authenticationService: AuthenticationService =
        (_: String, _: String) => ServiceCall { _ => Future.successful("admin", AuthenticationRole.Admin) }

      override lazy val hyperLedgerService: HyperLedgerService = new HyperLedgerService {
        private var data: List[String] = List()

        override def write(transactionId: String): ServiceCall[Seq[String], Done] = ServiceCall { seq =>
          transactionId match {
            case "addStudent" =>
              data ++= seq
              Future.successful(Done)
            case _ => Future.successful(Done)
          }
        }

        override def read(transactionId: String): ServiceCall[Seq[String], String] = ServiceCall { seq =>
          transactionId match {
            case "getStudent" =>
              val mat = data.find(s => s.contains(seq[0]))
              if(mat.isDefined){
                Future.successful(mat.get)
              } else {
                throw new CustomException(TransportErrorCode(404, 1003, "Error"), new DetailedError("not found", "There is no student for the given matriculationId.", Seq()))
              }
            case _ => Future.successful("")
          }
        }
      }
    }
  }

  val client: MatriculationService = server.serviceClient.implement[MatriculationService]

  override protected def afterAll(): Unit = server.stop()

  def addAuthenticationHeader(): RequestHeader => RequestHeader = { header =>
    header.withHeader("Authorization", "Basic " + Base64.getEncoder.encodeToString("MOCK:MOCK".getBytes()))
  }

  /** Tests only working if the whole instance is started */
  "MatriculationService service" should {

  }
}
