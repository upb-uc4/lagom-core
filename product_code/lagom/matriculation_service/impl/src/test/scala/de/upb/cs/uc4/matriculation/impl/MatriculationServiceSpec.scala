package de.upb.cs.uc4.matriculation.impl

import java.util.Base64

import akka.Done
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport.RequestHeader
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import de.upb.cs.uc4.authentication.api.AuthenticationService
import de.upb.cs.uc4.authentication.model.AuthenticationRole
import de.upb.cs.uc4.hyperledger.api.HyperLedgerService
import de.upb.cs.uc4.matriculation.api.MatriculationService
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

import scala.concurrent.Future

/** Tests for the MatriculationService
 *
 * All tests need to be started in the defined order
 */
class MatriculationServiceSpec extends AsyncWordSpec with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  private val server = ServiceTest.startServer(
    ServiceTest.defaultSetup
  ) { ctx =>
    new MatriculationApplication(ctx) with LocalServiceLocator {
      override lazy val authenticationService: AuthenticationService =
        (_: String, _: String) => ServiceCall { _ => Future.successful("admin", AuthenticationRole.Admin) }

      override lazy val hyperLedgerService: HyperLedgerService = new HyperLedgerService {
        override def write(transactionId: String): ServiceCall[Seq[String], Done] = ServiceCall { seq =>
          transactionId match {
            case _ => Future.successful(Done)
          }
        }

        override def read(transactionId: String): ServiceCall[Seq[String], String] = ServiceCall { seq =>
          transactionId match {
            case "" => Future.successful("")
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
