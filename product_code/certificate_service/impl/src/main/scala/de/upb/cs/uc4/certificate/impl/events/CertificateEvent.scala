package de.upb.cs.uc4.certificate.impl.events

import com.lightbend.lagom.scaladsl.persistence.{ AggregateEvent, AggregateEventTag }

/** The trait for the events needed in the state
  * Every event is a case class containing the
  * necessary information to apply the event
  */
trait CertificateEvent extends AggregateEvent[CertificateEvent] {
  def aggregateTag: AggregateEventTag[CertificateEvent] = CertificateEvent.Tag
}

object CertificateEvent {
  val Tag: AggregateEventTag[CertificateEvent] = AggregateEventTag[CertificateEvent]
}
