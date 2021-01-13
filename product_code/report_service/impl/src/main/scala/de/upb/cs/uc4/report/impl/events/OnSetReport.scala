package de.upb.cs.uc4.report.impl.events

import de.upb.cs.uc4.report.impl.actor.TextReport
import play.api.libs.json.{ Format, Json }

case class OnSetReport(textReport: TextReport, profilePicture: Array[Byte], thumbnail: Array[Byte], timestamp: String) extends ReportEvent

object OnSetReport {
  implicit val format: Format[OnSetReport] = Json.format
}