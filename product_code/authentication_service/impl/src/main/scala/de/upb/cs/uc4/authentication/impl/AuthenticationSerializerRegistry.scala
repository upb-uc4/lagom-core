package de.upb.cs.uc4.authentication.impl

import com.lightbend.lagom.scaladsl.playjson.JsonSerializer
import de.upb.cs.uc4.authentication.impl.actor.{ AuthenticationEntry, AuthenticationState }
import de.upb.cs.uc4.authentication.impl.events.{ OnDelete, OnSet }
import de.upb.cs.uc4.shared.server.SharedSerializerRegistry

object AuthenticationSerializerRegistry extends SharedSerializerRegistry {
  /** All Service specific JsonSerializers */
  override def customSerializers: Seq[JsonSerializer[_]] = Seq(
    JsonSerializer[OnDelete],
    JsonSerializer[OnSet],
    JsonSerializer[AuthenticationState],
    JsonSerializer[AuthenticationEntry]
  )
}
