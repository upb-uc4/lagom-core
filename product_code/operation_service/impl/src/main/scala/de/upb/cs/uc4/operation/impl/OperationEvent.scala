package de.upb.cs.uc4.operation.impl

import com.lightbend.lagom.scaladsl.persistence.{ AggregateEvent, AggregateEventTag }

/** The trait for the events needed in the state
  * Every event is a case class containing the
  * necessary information to apply the event
  */
trait OperationEvent extends AggregateEvent[OperationEvent] {
  def aggregateTag: AggregateEventTag[OperationEvent] = OperationEvent.Tag
}

object OperationEvent {
  val Tag: AggregateEventTag[OperationEvent] = AggregateEventTag[OperationEvent]
}