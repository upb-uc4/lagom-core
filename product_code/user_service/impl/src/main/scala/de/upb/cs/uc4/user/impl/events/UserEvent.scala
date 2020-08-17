package de.upb.cs.uc4.user.impl.events

import com.lightbend.lagom.scaladsl.persistence.{ AggregateEvent, AggregateEventTag }

/** The trait for the events needed in the state
  * Every event is a case class containing the
  * necessary information to apply the event
  */
trait UserEvent extends AggregateEvent[UserEvent] {
  def aggregateTag: AggregateEventTag[UserEvent] = UserEvent.Tag
}

object UserEvent {
  val Tag: AggregateEventTag[UserEvent] = AggregateEventTag[UserEvent]
}
