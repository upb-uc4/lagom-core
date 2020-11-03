package de.upb.cs.uc4.shared.client

import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.transport.{ MessageProtocol, Method, RequestHeader, ResponseHeader }
import com.lightbend.lagom.scaladsl.api.{ Descriptor, Service, ServiceAcl, ServiceCall }
import de.upb.cs.uc4.shared.client.JsonUtility._
import de.upb.cs.uc4.shared.client.exceptions.{ UC4Exception, UC4ExceptionSerializer }
import play.api.Environment
import play.api.libs.json.Writes

import scala.concurrent.Future

trait UC4Service extends Service {
  /** Prefix for the path for the endpoints, a name/identifier for the service */
  val pathPrefix: String
  /** The name of the service */
  val name: String
  /** This services uses auto acl (Default: True) */
  val autoAcl: Boolean = true

  private lazy val versionNumber = getClass.getPackage.getImplementationVersion

  /** Returns the current Version of this service */
  def getVersionNumber: ServiceCall[NotUsed, JsonVersionNumber] = ServiceCall { _ =>
    Future.successful(JsonVersionNumber(versionNumber))
  }

  /** This Methods needs to allow a GET-Method */
  def allowVersionNumber: ServiceCall[NotUsed, Done]

  override def descriptor: Descriptor = {
    import Service._
    val descriptor = named(name)
      .withCalls(
        restCall(Method.GET, pathPrefix + "/version", getVersionNumber _),
        restCall(Method.OPTIONS, pathPrefix + "/version", allowVersionNumber _)
      )
      .withExceptionSerializer(
        new UC4ExceptionSerializer(Environment.simple())
      )

    if (autoAcl) {
      descriptor.withAutoAcl(true)
    }
    else {
      descriptor.withAcls(
        ServiceAcl.forMethodAndPathRegex(Method.GET, "\\Q" + pathPrefix + "/version\\E"),
        ServiceAcl.forMethodAndPathRegex(Method.OPTIONS, "\\Q" + pathPrefix + "/version\\E")
      )
    }
  }

  protected def addAuthenticationHeader(serviceHeader: RequestHeader): RequestHeader => RequestHeader = {
    origin =>
      origin
        .addHeader("Cookie", serviceHeader.getHeader("Cookie").getOrElse(""))
        .addHeader("Authorization", serviceHeader.getHeader("Authorization").getOrElse(""))
  }

  protected def checkETag[T](serviceHeader: RequestHeader, obj: T)(implicit writes: Writes[T]): String = {
    val eTag = serviceHeader.getHeader("If-None-Match").getOrElse("")
    val newTag = Hashing.sha256(obj.toJson)

    if (newTag == eTag) {
      throw UC4Exception.NotModified
    }
    else {
      newTag
    }
  }

  protected def createETagHeader[T](requestHeader: RequestHeader, obj: T, code: Int = 200, headers: List[(String, String)] = List())(implicit writes: Writes[T]): (ResponseHeader, T) =
    (ResponseHeader(code, MessageProtocol.empty, headers).addHeader("ETag", checkETag(requestHeader, obj)), obj)
}
