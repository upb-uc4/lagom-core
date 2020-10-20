package de.upb.cs.uc4.configuration.model

import play.api.libs.json.{ Format, Json }

case class Configuration(fieldsOfStudy: Seq[String], countries: Seq[String], languages: Seq[String], courseTypes: Seq[String])

object Configuration {
  implicit val format: Format[Configuration] = Json.format
}