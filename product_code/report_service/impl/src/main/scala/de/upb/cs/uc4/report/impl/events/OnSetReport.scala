package de.upb.cs.uc4.report.impl.events

import de.upb.cs.uc4.report.impl.actor.Report
import play.api.libs.json.{ Format, Json }

case class OnSetReport(report: Report) extends ReportEvent

object OnSetReport {
  implicit val format: Format[OnSetReport] = Json.format
}