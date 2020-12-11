package de.upb.cs.uc4.report.impl.events

import com.lightbend.lagom.scaladsl.persistence.{ AggregateEvent, AggregateEventTag }

/** The trait for the events needed in the state
  * Every event is a case class containing the
  * necessary information to apply the event
  */
trait ReportEvent extends AggregateEvent[ReportEvent] {
  def aggregateTag: AggregateEventTag[ReportEvent] = ReportEvent.Tag
}

object ReportEvent {
  val Tag: AggregateEventTag[ReportEvent] = AggregateEventTag[ReportEvent]
}

