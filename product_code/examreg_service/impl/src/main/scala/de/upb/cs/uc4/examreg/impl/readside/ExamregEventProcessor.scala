package de.upb.cs.uc4.examreg.impl.readside

import com.lightbend.lagom.scaladsl.persistence.slick.SlickReadSide
import com.lightbend.lagom.scaladsl.persistence.{ AggregateEventTag, ReadSideProcessor }
import de.upb.cs.uc4.examreg.impl.ExamregApplication
import de.upb.cs.uc4.examreg.impl.events.{ ExamregEvent, OnExamregCreate }

class ExamregEventProcessor(readSide: SlickReadSide, database: ExamregDatabase)
  extends ReadSideProcessor[ExamregEvent] {

  /** @inheritdoc */
  override def buildHandler(): ReadSideProcessor.ReadSideHandler[ExamregEvent] =
    readSide.builder[ExamregEvent](ExamregApplication.offset)
      .setGlobalPrepare(database.createTable())
      .setEventHandler[OnExamregCreate] { envelope =>
        database.addExamreg(envelope.event.examreg)
      }
      .build()

  /** @inheritdoc */
  override def aggregateTags: Set[AggregateEventTag[ExamregEvent]] = Set(ExamregEvent.Tag)
}
