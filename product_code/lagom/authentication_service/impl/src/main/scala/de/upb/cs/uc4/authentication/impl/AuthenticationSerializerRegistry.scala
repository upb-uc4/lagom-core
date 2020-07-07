package de.upb.cs.uc4.authentication.impl

import com.lightbend.lagom.scaladsl.playjson.JsonSerializer
import de.upb.cs.uc4.shared.server.SharedSerializerRegistry

object AuthenticationSerializerRegistry extends SharedSerializerRegistry {
  /** All Service specific JsonSerializers */
  override def customSerializers: Seq[JsonSerializer[_]] = Seq()
}
