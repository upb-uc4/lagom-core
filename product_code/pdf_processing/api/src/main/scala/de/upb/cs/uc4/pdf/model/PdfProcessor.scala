package de.upb.cs.uc4.pdf.model

import play.api.libs.json.{ Format, Json }

case class PdfProcessor(content: String, options: Map[String, String])

object PdfProcessor {

  def apply(content: String): PdfProcessor = new PdfProcessor(content, Map())

  implicit val format: Format[PdfProcessor] = Json.format
}
