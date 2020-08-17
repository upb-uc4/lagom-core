package de.upb.cs.uc4.matriculation.model

import play.api.libs.json.{ Format, Json }

case class SubjectMatriculation(fieldOfStudy: String, semesters: Seq[String]) {
  def trim: SubjectMatriculation =
    copy(fieldOfStudy.trim, semesters.map(_.trim))
}

object SubjectMatriculation {
  implicit val format: Format[SubjectMatriculation] = Json.format
}