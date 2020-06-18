package de.upb.cs.uc4.course.impl.readside

import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}
import de.upb.cs.uc4.course.impl.CourseApplication
import de.upb.cs.uc4.course.impl.events.{CourseEvent, OnCourseCreate, OnCourseDelete}

class CourseEventProcessor(readSide: CassandraReadSide, database: CourseDatabase)
  extends ReadSideProcessor[CourseEvent]{

  /** @inheritdoc */
  override def buildHandler(): ReadSideProcessor.ReadSideHandler[CourseEvent] =
    readSide.builder[CourseEvent](CourseApplication.cassandraOffset)
      .setGlobalPrepare(database.globalPrepare)
      .setPrepare(database.prepare)
      .setEventHandler[OnCourseCreate](database.addCourse)
      .setEventHandler[OnCourseDelete](database.deleteCourse)
      .build()

  /** @inheritdoc */
  override def aggregateTags: Set[AggregateEventTag[CourseEvent]] = Set(CourseEvent.Tag)
}
