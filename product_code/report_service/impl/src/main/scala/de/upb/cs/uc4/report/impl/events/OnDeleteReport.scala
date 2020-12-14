package de.upb.cs.uc4.report.impl.events

import de.upb.cs.uc4.report.impl.actor.Report
import play.api.libs.json.{ Format, Json }

case class OnDeleteReport(report: Report) extends ReportEvent

object OnDeleteReport {
  implicit val format: Format[OnDeleteReport] = Json.format
}

