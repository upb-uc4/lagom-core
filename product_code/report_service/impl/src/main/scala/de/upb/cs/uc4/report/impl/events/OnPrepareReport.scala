package de.upb.cs.uc4.report.impl.events

import play.api.libs.json.{ Format, Json }

case class OnPrepareReport(timestamp: String) extends ReportEvent

object OnPrepareReport {
  implicit val format: Format[OnPrepareReport] = Json.format
}