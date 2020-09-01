package de.upb.cs.uc4.user.impl.actor

import akka.actor.typed.Behavior
import akka.cluster.sharding.typed.scaladsl.EntityContext
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.EventSourcedBehavior
import com.lightbend.lagom.scaladsl.persistence.AkkaTaggerAdapter
import de.upb.cs.uc4.user.impl.commands.UserCommand
import de.upb.cs.uc4.user.impl.events.UserEvent

object UserBehaviour {

  /** Given a sharding [[EntityContext]] this function produces an Akka [[Behavior]] for the aggregate.
    */
  def create(entityContext: EntityContext[UserCommand]): Behavior[UserCommand] = {
    val persistenceId: PersistenceId = PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId)

    create(persistenceId)
      .withTagger(
        // Using Akka Persistence Typed in Lagom requires tagging your events
        // in Lagom-compatible way so Lagom ReadSideProcessors and TopicProducers
        // can locate and follow the event streams.
        AkkaTaggerAdapter.fromLagom(entityContext, UserEvent.Tag)
      )

  }
  /*
   * This method is extracted to write unit tests that are completely independent to Akka Cluster.
   */
  private[impl] def create(persistenceId: PersistenceId): EventSourcedBehavior[UserCommand, UserEvent, UserState] = EventSourcedBehavior
    .withEnforcedReplies[UserCommand, UserEvent, UserState](
      persistenceId = persistenceId,
      emptyState = UserState.initial,
      commandHandler = (state, cmd) => state.applyCommand(cmd),
      eventHandler = (state, evt) => state.applyEvent(evt)
    )
}
