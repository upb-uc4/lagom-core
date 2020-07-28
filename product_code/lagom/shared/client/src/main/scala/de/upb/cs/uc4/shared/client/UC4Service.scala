package de.upb.cs.uc4.shared.client

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceAcl, ServiceCall}
import play.api.Environment

import scala.concurrent.Future

trait UC4Service extends Service {
  /** Prefix for the path for the endpoints, a name/identifier for the service */
  val pathPrefix: String
  /** The name of the service */
  val name: String

  private lazy val versionNumber = getClass.getPackage.getImplementationVersion

  /** Returns the current Version of this service */
  def getVersionNumber: ServiceCall[NotUsed, JsonVersionNumber] = ServiceCall { _ =>
    Future.successful(JsonVersionNumber(versionNumber))
  }

  def allowVersionNumber: ServiceCall[NotUsed, Done]

  override def descriptor: Descriptor = {
    import Service._
    named(name)
      .withCalls(
        restCall(Method.GET, pathPrefix + "/version", getVersionNumber _),
        restCall(Method.OPTIONS, pathPrefix + "/version", allowVersionNumber _),
      )
      .withAcls(
        ServiceAcl.forMethodAndPathRegex(Method.GET, "\\Q" + pathPrefix + "/version\\E"),
        ServiceAcl.forMethodAndPathRegex(Method.OPTIONS, "\\Q" + pathPrefix + "/version\\E"),
      )
      .withExceptionSerializer(
        new CustomExceptionSerializer(Environment.simple())
      )
  }
}