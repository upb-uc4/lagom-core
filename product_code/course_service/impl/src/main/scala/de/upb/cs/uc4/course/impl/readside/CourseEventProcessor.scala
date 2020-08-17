package de.upb.cs.uc4.course.impl.readside

import com.lightbend.lagom.scaladsl.persistence.slick.SlickReadSide
import com.lightbend.lagom.scaladsl.persistence.{ AggregateEventTag, ReadSideProcessor }
import de.upb.cs.uc4.course.impl.CourseApplication
import de.upb.cs.uc4.course.impl.events.{ CourseEvent, OnCourseCreate, OnCourseDelete }

class CourseEventProcessor(readSide: SlickReadSide, database: CourseDatabase)
  extends ReadSideProcessor[CourseEvent] {

  /** @inheritdoc */
  override def buildHandler(): ReadSideProcessor.ReadSideHandler[CourseEvent] =
    readSide.builder[CourseEvent](CourseApplication.offset)
      .setGlobalPrepare(database.createTable())
      .setEventHandler[OnCourseCreate] { envelope =>
        database.addCourse(envelope.event.course)
      }
      .setEventHandler[OnCourseDelete] { envelope =>
        database.removeCourse(envelope.event.id)
      }
      .build()

  /** @inheritdoc */
  override def aggregateTags: Set[AggregateEventTag[CourseEvent]] = Set(CourseEvent.Tag)
}
