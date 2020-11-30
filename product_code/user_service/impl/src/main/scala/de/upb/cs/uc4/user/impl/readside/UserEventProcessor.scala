package de.upb.cs.uc4.user.impl.readside

import com.lightbend.lagom.scaladsl.persistence.slick.SlickReadSide
import com.lightbend.lagom.scaladsl.persistence.{ AggregateEventTag, ReadSideProcessor }
import de.upb.cs.uc4.user.impl.UserApplication
import de.upb.cs.uc4.user.impl.events.{ OnUserCreate, OnUserForceDelete, UserEvent }

class UserEventProcessor(readSide: SlickReadSide, database: UserDatabase)
  extends ReadSideProcessor[UserEvent] {

  /** @inheritdoc */
  override def buildHandler(): ReadSideProcessor.ReadSideHandler[UserEvent] =
    readSide.builder[UserEvent](UserApplication.offset)
      .setGlobalPrepare(database.createTable())
      .setEventHandler[OnUserCreate] { envelope =>
        database.addUser(envelope.event.user)
      }
      .setEventHandler[OnUserForceDelete] { envelope =>
        database.removeUser(envelope.event.user) >>
          database.deleteImageQuery(envelope.event.user.username)
      }
      .build()

  /** @inheritdoc */
  override def aggregateTags: Set[AggregateEventTag[UserEvent]] = Set(UserEvent.Tag)
}
