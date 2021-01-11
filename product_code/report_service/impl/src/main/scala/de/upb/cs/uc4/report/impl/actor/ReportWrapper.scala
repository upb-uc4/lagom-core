package de.upb.cs.uc4.report.impl.actor

import de.upb.cs.uc4.report.impl.actor.ReportStateEnum.ReportStateEnum
import play.api.libs.json.{ Format, Json }

case class ReportWrapper(textReport: Option[TextReport], profilePicture: Option[Array[Byte]], thumbnail: Option[Array[Byte]], timestamp: Option[String], state: ReportStateEnum)

object ReportWrapper {
  implicit val format: Format[ReportWrapper] = Json.format
}