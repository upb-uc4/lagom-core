package de.upb.cs.uc4.examreg.impl.events

import com.lightbend.lagom.scaladsl.persistence.{ AggregateEvent, AggregateEventTag }

/** The trait for the events needed in the state
  * Every event is a case class containing the
  * necessary information to apply the event
  */
trait ExamregEvent extends AggregateEvent[ExamregEvent] {
  def aggregateTag: AggregateEventTag[ExamregEvent] = ExamregEvent.Tag
}

object ExamregEvent {
  val Tag: AggregateEventTag[ExamregEvent] = AggregateEventTag[ExamregEvent]
}

