package de.upb.cs.uc4.authentication.impl.readside

import com.lightbend.lagom.scaladsl.persistence.slick.SlickReadSide
import com.lightbend.lagom.scaladsl.persistence.{ AggregateEventTag, ReadSideProcessor }
import de.upb.cs.uc4.authentication.impl.AuthenticationApplication
import de.upb.cs.uc4.authentication.impl.events.{ AuthenticationEvent, OnDelete, OnSet }

class AuthenticationEventProcessor(readSide: SlickReadSide, database: AuthenticationDatabase)
  extends ReadSideProcessor[AuthenticationEvent] {

  /** @inheritdoc */
  override def buildHandler(): ReadSideProcessor.ReadSideHandler[AuthenticationEvent] =
    readSide.builder[AuthenticationEvent](AuthenticationApplication.offset)
      .setGlobalPrepare(database.createTable())
      .setEventHandler[OnSet] { envelope =>
        database.addAuthenticationUser(envelope.event.user)
      }
      .setEventHandler[OnDelete] { envelope =>
        database.removeAuthenticationUser(envelope.event.username)
      }
      .build()

  /** @inheritdoc */
  override def aggregateTags: Set[AggregateEventTag[AuthenticationEvent]] = Set(AuthenticationEvent.Tag)
}
