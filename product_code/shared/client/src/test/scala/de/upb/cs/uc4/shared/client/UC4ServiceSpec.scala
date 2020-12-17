package de.upb.cs.uc4.shared.client

import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport.{ RequestHeader, ResponseHeader }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.Writes
import de.upb.cs.uc4.shared.client.Hashing.sha256
import de.upb.cs.uc4.shared.client.JsonUtility.ToJsonUtil
import de.upb.cs.uc4.shared.client.exceptions.{ ErrorType, UC4Exception }

import scala.concurrent.Future

class UC4ServiceSpec extends AnyWordSpecLike with Matchers {

  object UC4ServiceTest extends UC4Service {
    override val pathPrefix: String = "/test"
    override val name: String = "test"

    override def allowVersionNumber: ServiceCall[NotUsed, Done] = _ => Future.successful(Done)

    def publicCheckETag[T](serviceHeader: RequestHeader, obj: T)(implicit writes: Writes[T]): String =
      checkETag(serviceHeader, obj)

    def publicCreateETagHeader[T](requestHeader: RequestHeader, obj: T, code: Int = 200, headers: List[(String, String)] = List())(implicit writes: Writes[T]): (ResponseHeader, T) =
      createETagHeader(requestHeader, obj, code, headers)
  }

  "UC4Service" should {

    "return NotModified response on matching ETags" in {
      val version = JsonServiceVersion("test")
      val header = RequestHeader.Default.addHeader("If-None-Match", sha256(version.toJson))

      val exception = the[UC4Exception] thrownBy UC4ServiceTest.publicCheckETag(header, version)
      exception.possibleErrorResponse.`type` should ===(ErrorType.NotModified)
    }

    "return a new ETag on non-matching ETags" in {
      val version1 = JsonServiceVersion("test1")
      val version2 = JsonServiceVersion("test2")
      val header = RequestHeader.Default.addHeader("If-None-Match", sha256(version1.toJson))

      UC4ServiceTest.publicCheckETag(header, version2) should ===(sha256(version2.toJson))
    }

    "return NotModified response on matching ETags during header creation" in {
      val version = JsonServiceVersion("test")
      val header = RequestHeader.Default.addHeader("If-None-Match", sha256(version.toJson))

      val exception = the[UC4Exception] thrownBy UC4ServiceTest.publicCreateETagHeader(header, version)
      exception.possibleErrorResponse.`type` should ===(ErrorType.NotModified)
    }

    "create a response header on non-matching ETags" in {
      val version1 = JsonServiceVersion("test1")
      val version2 = JsonServiceVersion("test2")
      val header = RequestHeader.Default.addHeader("If-None-Match", sha256(version1.toJson))

      UC4ServiceTest.publicCreateETagHeader(header, version2)._1.getHeader("ETag").get should ===(sha256(version2.toJson))
    }
  }
}
