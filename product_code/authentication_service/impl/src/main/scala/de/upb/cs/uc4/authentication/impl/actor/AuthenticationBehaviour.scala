package de.upb.cs.uc4.authentication.impl.actor

import akka.actor.typed.Behavior
import akka.cluster.sharding.typed.scaladsl.EntityContext
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.EventSourcedBehavior
import com.lightbend.lagom.scaladsl.persistence.AkkaTaggerAdapter
import de.upb.cs.uc4.authentication.impl.commands.AuthenticationCommand
import de.upb.cs.uc4.authentication.impl.events.AuthenticationEvent

object AuthenticationBehaviour {

  /** Given a sharding [[EntityContext]] this function produces an Akka [[Behavior]] for the aggregate.
    */
  def create(entityContext: EntityContext[AuthenticationCommand]): Behavior[AuthenticationCommand] = {
    val persistenceId: PersistenceId = PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId)

    create(persistenceId)
      .withTagger(
        // Using Akka Persistence Typed in Lagom requires tagging your events
        // in Lagom-compatible way so Lagom ReadSideProcessors and TopicProducers
        // can locate and follow the event streams.
        AkkaTaggerAdapter.fromLagom(entityContext, AuthenticationEvent.Tag)
      )

  }
  /*
   * This method is extracted to write unit tests that are completely independent to Akka Cluster.
   */
  private[impl] def create(persistenceId: PersistenceId): EventSourcedBehavior[AuthenticationCommand, AuthenticationEvent, AuthenticationState] = EventSourcedBehavior
    .withEnforcedReplies[AuthenticationCommand, AuthenticationEvent, AuthenticationState](
      persistenceId = persistenceId,
      emptyState = AuthenticationState.initial,
      commandHandler = (state, cmd) => state.applyCommand(cmd),
      eventHandler = (state, evt) => state.applyEvent(evt)
    )
}
