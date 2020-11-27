package de.upb.cs.uc4.admission.model

import play.api.libs.json.{ Format, Json }

case class DropAdmission(admissionId: String) {

  def trim: DropAdmission = copy(admissionId.trim)
}

object DropAdmission {
  implicit val format: Format[DropAdmission] = Json.format
}
