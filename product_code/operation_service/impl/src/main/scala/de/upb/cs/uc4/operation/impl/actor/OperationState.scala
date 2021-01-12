package de.upb.cs.uc4.operation.impl.actor

import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import akka.persistence.typed.scaladsl.{ Effect, ReplyEffect }
import de.upb.cs.uc4.operation.impl.OperationApplication
import de.upb.cs.uc4.operation.impl.commands.{ AddToWatchlist, GetWatchlist, OperationCommand, RemoveFromWatchlist }
import de.upb.cs.uc4.operation.impl.events.{ OnAddToWatchlist, OnRemoveFromWatchlist, OperationEvent }
import de.upb.cs.uc4.shared.server.messages.Accepted
import play.api.libs.json.{ Format, Json }

/** The current state of a ExaminationRegulation */
case class OperationState(watchlistWrapper: WatchlistWrapper) {

  /** Functions as a CommandHandler
    *
    * @param cmd the given command
    */
  def applyCommand(cmd: OperationCommand): ReplyEffect[OperationEvent, OperationState] =
    cmd match {
      case GetWatchlist(replyTo) =>
        Effect.reply(replyTo)(watchlistWrapper)
      case AddToWatchlist(operationId, replyTo) =>
        Effect.persist(OnAddToWatchlist(operationId)).thenReply(replyTo) { _ => Accepted.default }
      case RemoveFromWatchlist(operationId, replyTo) =>
        Effect.persist(OnRemoveFromWatchlist(operationId)).thenReply(replyTo) { _ => Accepted.default }
      case _ =>
        println("Unknown Command")
        Effect.noReply
    }

  /** Functions as an EventHandler
    *
    * @param evt the given event
    */
  def applyEvent(evt: OperationEvent): OperationState =
    evt match {
      case OnAddToWatchlist(operationId) =>
        copy(watchlistWrapper.addedToList(operationId))
      case OnRemoveFromWatchlist(operationId) =>
        copy(watchlistWrapper.removedFromList(operationId))
      case _ =>
        println("Unknown Event")
        this
    }
}

object OperationState {

  /** The initial state. This is used if there is no snapshotted state to be found.
    */
  def initial: OperationState = OperationState(WatchlistWrapper(Seq()))

  /** The [[akka.persistence.typed.scaladsl.EventSourcedBehavior]] instances (aka Aggregates) run on sharded actors inside the Akka Cluster.
    * When sharding actors and distributing them across the cluster, each aggregate is
    * namespaced under a typekey that specifies a name and also the type of the commands
    * that sharded actor can receive.
    */
  val typeKey: EntityTypeKey[OperationCommand] = EntityTypeKey[OperationCommand](OperationApplication.offset)

  /** Format for the course state.
    *
    * Persisted entities get snapshotted every configured number of events. This
    * means the state gets stored to the database, so that when the aggregate gets
    * loaded, you don't need to replay all the events, just the ones since the
    * snapshot. Hence, a JSON format needs to be declared so that it can be
    * serialized and deserialized when storing to and from the database.
    */
  implicit val format: Format[OperationState] = Json.format
}


