package de.upb.cs.uc4.admission.model

import play.api.libs.json.{ Format, Json }

case object AdmissionType extends Enumeration {
  type AdmissionType = Value
  val Course, Exam = Value

  implicit val format: Format[AdmissionType] = Json.formatEnum(this)

  def All: Seq[AdmissionType] = values.toSeq
}
