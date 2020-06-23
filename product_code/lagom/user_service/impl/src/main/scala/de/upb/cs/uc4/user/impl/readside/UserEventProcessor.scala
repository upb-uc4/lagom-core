package de.upb.cs.uc4.user.impl.readside

import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}
import de.upb.cs.uc4.user.impl.UserApplication
import de.upb.cs.uc4.user.impl.events.UserEvent

class UserEventProcessor(readSide: CassandraReadSide, database: UserDatabase)
  extends ReadSideProcessor[UserEvent]{

  /** @inheritdoc */
  override def buildHandler(): ReadSideProcessor.ReadSideHandler[UserEvent] =
    readSide.builder[UserEvent](UserApplication.cassandraOffset)
      .setGlobalPrepare(database.globalPrepare)
      .setPrepare(database.prepare)
      .setEventHandler[OnCourseCreate](database.addUser)
      .setEventHandler[OnCourseDelete](database.deleteUser)
      .build()

  /** @inheritdoc */
  override def aggregateTags: Set[AggregateEventTag[UserEvent]] = Set(UserEvent.Tag)
}
