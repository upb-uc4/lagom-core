package de.upb.cs.uc4.course.impl.events

import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag}

/** The trait for the events needed in the state
  * Every event is a case class containing the
  * necessary information to apply the event
  */
trait CourseEvent extends AggregateEvent[CourseEvent] {
  def aggregateTag: AggregateEventTag[CourseEvent] = CourseEvent.Tag
}

object CourseEvent {
  val Tag: AggregateEventTag[CourseEvent] = AggregateEventTag[CourseEvent]
}
