package de.upb.cs.uc4.operation.impl.readside

import com.lightbend.lagom.scaladsl.persistence.slick.SlickReadSide
import com.lightbend.lagom.scaladsl.persistence.{ AggregateEventTag, ReadSideProcessor }
import de.upb.cs.uc4.operation.impl.OperationApplication
import de.upb.cs.uc4.operation.impl.events.OperationEvent

class OperationEventProcessor(readSide: SlickReadSide)
  extends ReadSideProcessor[OperationEvent] {

  /** @inheritdoc */
  override def buildHandler(): ReadSideProcessor.ReadSideHandler[OperationEvent] =
    readSide.builder[OperationEvent](OperationApplication.offset)
      .build()

  /** @inheritdoc */
  override def aggregateTags: Set[AggregateEventTag[OperationEvent]] = Set(OperationEvent.Tag)
}
