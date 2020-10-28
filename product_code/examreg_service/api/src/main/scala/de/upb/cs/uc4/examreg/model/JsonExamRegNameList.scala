package de.upb.cs.uc4.examreg.model

import play.api.libs.json.{ Format, Json }

case class JsonExamRegNameList(examinationRegulations: Seq[String])

object JsonExamRegNameList {
  implicit val format: Format[JsonExamRegNameList] = Json.format
}