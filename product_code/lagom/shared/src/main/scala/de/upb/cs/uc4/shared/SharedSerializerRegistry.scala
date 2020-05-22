package de.upb.cs.uc4.shared

import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import de.upb.cs.uc4.shared.messages.{Accepted, Confirmation, Rejected}

import scala.collection.immutable.Seq

/** Automatically creates Serializers for all shared messages, events etc */
trait SharedSerializerRegistry extends JsonSerializerRegistry {
  override def serializers: Seq[JsonSerializer[_]] = Seq(
    JsonSerializer[Confirmation],
    JsonSerializer[Accepted],
    JsonSerializer[Rejected],
  ) ++ customSerializers

  /** All Service specific JsonSerializers */
  def customSerializers: Seq[JsonSerializer[_]]
}
