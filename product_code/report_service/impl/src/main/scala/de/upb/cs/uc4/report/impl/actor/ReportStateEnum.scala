package de.upb.cs.uc4.report.impl.actor

import play.api.libs.json.{ Format, Json }

object ReportStateEnum extends Enumeration {
  type ReportStateEnum = Value
  val None, Preparing, Ready = Value

  implicit val format: Format[ReportStateEnum] = Json.formatEnum(this)

  def All: Seq[ReportStateEnum] = values.toSeq
}