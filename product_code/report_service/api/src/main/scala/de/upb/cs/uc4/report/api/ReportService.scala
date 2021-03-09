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
  //def prepareUserData(username: String): ServiceCall[NotUsed, Done]

  /** Get all data for the specified user */
  def getUserReport(username: String): ServiceCall[NotUsed, ByteString]

  /** Delete a user's report */
  def deleteUserReport(username: String): ServiceCall[NotUsed, Done]

  /** Returns a pdf with the certificate of enrollment */
  def getCertificateOfEnrollment(username: String, semesterBase64: Option[String]): ServiceCall[NotUsed, ByteString]

  /** Returns a pdf with the transcript of records */
  def getTranscriptOfRecords(username: String, examRegName: Option[String]): ServiceCall[NotUsed, ByteString]

  /** Allows GET */
  def allowedGet: ServiceCall[NotUsed, Done]

  /** Allows GET */
  def allowedMethodsGETDELETE: ServiceCall[NotUsed, Done]

  final override def descriptor: Descriptor = {
    import Service._
    super.descriptor
      .addCalls(
        restCall(Method.GET, pathPrefix + "/reports/:username/archive", getUserReport _),
        restCall(Method.DELETE, pathPrefix + "/reports/:username/archive", deleteUserReport _),
        restCall(Method.GET, pathPrefix + "/certificates/:username/enrollment?semester", getCertificateOfEnrollment _),
        restCall(Method.GET, pathPrefix + "/certificates/:username/transcript_of_records?exam_reg_name", getTranscriptOfRecords _),

        restCall(Method.OPTIONS, pathPrefix + "/reports/:username/archive", allowedMethodsGETDELETE _),
        restCall(Method.OPTIONS, pathPrefix + "/certificates/:username/enrollment?semester", allowedGet _),
        restCall(Method.OPTIONS, pathPrefix + "/certificates/:username/transcript_of_records?exam_reg_name", allowedGet _)
      )
  }
}
