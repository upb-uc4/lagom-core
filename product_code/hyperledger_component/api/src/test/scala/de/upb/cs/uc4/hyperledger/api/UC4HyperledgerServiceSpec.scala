package de.upb.cs.uc4.hyperledger.api

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.typesafe.config.{Config, ConfigFactory}
import de.upb.cs.uc4.hyperledger.api.model.{JsonHyperledgerVersion, UnsignedProposal, UnsignedTransaction}
import io.jsonwebtoken.Jwts
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.io.File
import java.util.{Base64, Calendar, Date}
import scala.concurrent.Future

class UC4HyperledgerServiceSpec extends AnyWordSpec with Matchers {

  private implicit val applicationConfig: Config = {
    ConfigFactory.parseFile(new File(getClass.getResource("/test-application.conf").getPath))
  }

  protected lazy val jwtKey: String = applicationConfig.getString("uc4.hyperledger.jwtKey")
  protected lazy val processingTime: Int = applicationConfig.getInt("uc4.hyperledger.processingTime")

  val hyperledgerService: UC4HyperledgerService = new UC4HyperledgerService {
    override val pathPrefix: String = "mock"
    override val name: String = "mock"
    override val config: Config = applicationConfig

    override def createTimedUnsignedProposal(unsignedProposal: Array[Byte]): UnsignedProposal =
      super.createTimedUnsignedProposal(unsignedProposal)

    override def createTimedUnsignedTransaction(unsignedTransaction: Array[Byte]): UnsignedTransaction =
      super.createTimedUnsignedTransaction(unsignedTransaction)

    override def getHlfVersions: ServiceCall[NotUsed, JsonHyperledgerVersion] =
      _ => Future.successful(JsonHyperledgerVersion("mock", "mock"))

    override def allowVersionNumber: ServiceCall[NotUsed, Done] =
      _ => Future.successful(Done)
  }

  private def checkDate(date: Date, expectedDate: Date): Assertion = {
    val afterDate = Calendar.getInstance()
    afterDate.setTime(expectedDate)
    afterDate.add(Calendar.SECOND, 2)

    val beforeDate = Calendar.getInstance()
    beforeDate.setTime(expectedDate)
    beforeDate.add(Calendar.SECOND, -2)

    date.before(afterDate.getTime) && date.after(beforeDate.getTime) shouldBe true
  }

  "A UC4HyperledgerService" should  {

    "create correct timed token, which" must {

      "have the correct subject" in {
        val token = hyperledgerService.createTimedToken(Array.emptyByteArray)

        val subject = Jwts.parser().setSigningKey(jwtKey).parseClaimsJws(token).getBody.getSubject

        subject should ===("timed")
      }

      "have the correct expiration date" in {
        val token = hyperledgerService.createTimedToken(Array.emptyByteArray)

        val date = Jwts.parser().setSigningKey(jwtKey).parseClaimsJws(token).getBody.getExpiration

        val expectedDate = Calendar.getInstance()
        expectedDate.add(Calendar.MINUTE, processingTime)
        checkDate(date, expectedDate.getTime)
      }

      "have the correct bytes" in {
        val testBytes = "This is a test".getBytes()

        val token = hyperledgerService.createTimedToken(testBytes)

        val base64 = Jwts.parser().setSigningKey(jwtKey).parseClaimsJws(token).getBody.get("unsignedBytes", classOf[String])
        val bytes = Base64.getDecoder.decode(base64)

        bytes should contain theSameElementsInOrderAs testBytes
      }
    }
  }
}
