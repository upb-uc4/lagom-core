package de.upb.cs.uc4.matriculation.model

import play.api.libs.json.{Format, Json}

case class PutMessageMatriculationData(fieldOfStudy: String, semester: String) {

  def trim: PutMessageMatriculationData = copy(fieldOfStudy.trim, semester.trim)
}

object PutMessageMatriculationData {
  implicit val format: Format[PutMessageMatriculationData] = Json.format
}
