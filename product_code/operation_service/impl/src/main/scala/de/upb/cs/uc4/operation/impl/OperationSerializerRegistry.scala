package de.upb.cs.uc4.operation.impl

import com.lightbend.lagom.scaladsl.playjson.JsonSerializer
import de.upb.cs.uc4.hyperledger.api.model.operation.{ ApprovalList, OperationData, TransactionInfo }
import de.upb.cs.uc4.operation.impl.actor.{ OperationDataList, OperationState, WatchlistWrapper }
import de.upb.cs.uc4.operation.impl.events.{ OnAddToWatchlist, OnRemoveFromWatchlist }
import de.upb.cs.uc4.operation.model.JsonRejectMessage
import de.upb.cs.uc4.hyperledger.api.model.operation.OperationDataState.OperationDataState
import de.upb.cs.uc4.shared.server.SharedSerializerRegistry

import scala.collection.immutable.Seq

/** Akka serialization, used by both persistence and remoting, needs to have
  * serializers registered for every type serialized or deserialized. While it's
  * possible to use any serializer you want for Akka messages, out of the box
  * Lagom provides support for JSON, via this registry abstraction.
  *
  * The serializers are registered here, and then provided to Lagom in the
  * application loader.
  */
object OperationSerializerRegistry extends SharedSerializerRegistry {
  override def customSerializers: Seq[JsonSerializer[_]] = Seq( // state and events can use play-json, but commands should use jackson because of ActorRef[T] (see application.conf)
    //Data
    JsonSerializer[OperationData],
    JsonSerializer[OperationDataState],
    JsonSerializer[OperationState],
    JsonSerializer[WatchlistWrapper],
    JsonSerializer[ApprovalList],
    // Messages
    JsonSerializer[OperationDataList],
    JsonSerializer[JsonRejectMessage],
    JsonSerializer[TransactionInfo],
    //Event
    JsonSerializer[OnAddToWatchlist],
    JsonSerializer[OnRemoveFromWatchlist]
  )
}
