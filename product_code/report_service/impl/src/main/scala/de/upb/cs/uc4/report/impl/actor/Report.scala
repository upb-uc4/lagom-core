package de.upb.cs.uc4.report.impl.actor

import play.api.libs.json.{ Format, Json }

case class Report(report: Array[Byte], timestamp: String)

object Report {
  implicit val format: Format[Report] = Json.format
}