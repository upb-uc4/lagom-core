package de.upb.cs.uc4.certificate.impl.readside

import com.lightbend.lagom.scaladsl.persistence.slick.SlickReadSide
import com.lightbend.lagom.scaladsl.persistence.{ AggregateEventTag, ReadSideProcessor }
import de.upb.cs.uc4.certificate.impl.CertificateApplication
import de.upb.cs.uc4.certificate.impl.events.CertificateEvent

class CertificateEventProcessor(readSide: SlickReadSide)
  extends ReadSideProcessor[CertificateEvent] {

  /** @inheritdoc */
  override def buildHandler(): ReadSideProcessor.ReadSideHandler[CertificateEvent] =
    readSide.builder[CertificateEvent](CertificateApplication.offset)
      .build()

  /** @inheritdoc */
  override def aggregateTags: Set[AggregateEventTag[CertificateEvent]] = Set(CertificateEvent.Tag)
}
