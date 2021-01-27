package de.upb.cs.uc4.pdf.api

import akka.util.ByteString
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{ Descriptor, Service, ServiceCall }
import de.upb.cs.uc4.pdf.model.PdfProcessor

trait PdfProcessingService extends Service {

  def convertHtml(): ServiceCall[PdfProcessor, ByteString]

  final override def descriptor: Descriptor = {
    import Service._
    named("pdfprocessing")
      .withCalls(
        restCall(Method.POST, "/", convertHtml _)
      )
      .withAutoAcl(false)
  }
}
