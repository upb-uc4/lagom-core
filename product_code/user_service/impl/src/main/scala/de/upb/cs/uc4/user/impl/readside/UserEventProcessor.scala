package de.upb.cs.uc4.user.impl.readside

import com.lightbend.lagom.scaladsl.persistence.slick.SlickReadSide
import com.lightbend.lagom.scaladsl.persistence.{ AggregateEventTag, ReadSideProcessor }
import de.upb.cs.uc4.user.impl.UserApplication
import de.upb.cs.uc4.user.impl.events.{ OnImageSet, OnUserCreate, OnUserDelete, UserEvent }

class UserEventProcessor(readSide: SlickReadSide, database: UserDatabase)
  extends ReadSideProcessor[UserEvent] {

  /** @inheritdoc */
  override def buildHandler(): ReadSideProcessor.ReadSideHandler[UserEvent] =
    readSide.builder[UserEvent](UserApplication.offset)
      .setGlobalPrepare(database.createTable())
      .setEventHandler[OnUserCreate] { envelope =>
        database.addUser(envelope.event.user)
      }
      .setEventHandler[OnUserDelete] { envelope =>
        database.removeUser(envelope.event.user)
      }
      .setEventHandler[OnImageSet] { envelope =>
        database.setImage(envelope.event.username, envelope.event.imagePath, envelope.event.contentType)
      }
      .build()

  /** @inheritdoc */
  override def aggregateTags: Set[AggregateEventTag[UserEvent]] = Set(UserEvent.Tag)
}
