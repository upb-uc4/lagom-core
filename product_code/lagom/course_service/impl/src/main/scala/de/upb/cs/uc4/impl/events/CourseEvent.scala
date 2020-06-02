package de.upb.cs.uc4.impl.events

import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag}

/**
  * This interface defines all the events that the Universitycredits4Aggregate supports.
  */
trait CourseEvent extends AggregateEvent[CourseEvent] {
  def aggregateTag: AggregateEventTag[CourseEvent] = CourseEvent.Tag
}

object CourseEvent {
  val Tag: AggregateEventTag[CourseEvent] = AggregateEventTag[CourseEvent]
}
