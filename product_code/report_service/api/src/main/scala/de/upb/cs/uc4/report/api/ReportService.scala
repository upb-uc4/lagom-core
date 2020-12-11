package de.upb.cs.uc4.report.api

import akka.util.ByteString
import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{ Descriptor, Service, ServiceCall }
import de.upb.cs.uc4.shared.client.UC4Service

/** The ReportService interface.
  *
  * This describes everything that Lagom needs to know about how to serve and
  * consume the ReportService.
  */
trait ReportService extends UC4Service {
  /** Prefix for the path for the endpoints, a name/identifier for the service */
  override val pathPrefix = "/report-management"
  override val name = "report"

  /** Request collection of all data for the given user */
  def prepareUserData(username: String): ServiceCall[NotUsed, Done]

  /** Get all data for the specified user */
  def getUserData(username: String): ServiceCall[NotUsed, ByteString]

  /** Allows GET */
  def allowedMethodsGET: ServiceCall[NotUsed, Done]

  final override def descriptor: Descriptor = {
    import Service._
    super.descriptor
      .addCalls(
        restCall(Method.GET, pathPrefix + "/reports/:username/prepare", prepareUserData _),
        restCall(Method.GET, pathPrefix + "/reports/:username/result", getUserData _),
        restCall(Method.OPTIONS, pathPrefix + "/reports/:username/prepare", allowedMethodsGET _),
        restCall(Method.OPTIONS, pathPrefix + "/reports/:username/result", allowedMethodsGET _)
      )
  }
}
