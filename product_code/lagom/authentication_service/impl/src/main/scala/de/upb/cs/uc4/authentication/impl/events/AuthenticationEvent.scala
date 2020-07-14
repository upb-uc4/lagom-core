package de.upb.cs.uc4.authentication.impl.events

import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag}

/** The trait for the events needed in the state
  * Every event is a case class containing the
  * necessary information to apply the event
  */
trait AuthenticationEvent extends AggregateEvent[AuthenticationEvent] {
  def aggregateTag: AggregateEventTag[AuthenticationEvent] = AuthenticationEvent.Tag
}

object AuthenticationEvent {
  val Tag: AggregateEventTag[AuthenticationEvent] = AggregateEventTag[AuthenticationEvent]
}
