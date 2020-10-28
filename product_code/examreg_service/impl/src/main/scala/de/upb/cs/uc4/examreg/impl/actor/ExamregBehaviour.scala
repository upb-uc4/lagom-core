package de.upb.cs.uc4.examreg.impl.actor

import akka.actor.typed.Behavior
import akka.cluster.sharding.typed.scaladsl.EntityContext
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.EventSourcedBehavior
import com.lightbend.lagom.scaladsl.persistence.AkkaTaggerAdapter
import de.upb.cs.uc4.examreg.impl.commands.ExamregCommand
import de.upb.cs.uc4.examreg.impl.events.ExamregEvent

object ExamregBehaviour {

  /** Given a sharding [[EntityContext]] this function produces an Akka [[Behavior]] for the aggregate.
    */
  def create(entityContext: EntityContext[ExamregCommand]): Behavior[ExamregCommand] = {
    val persistenceId: PersistenceId = PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId)

    create(persistenceId)
      .withTagger(
        // Using Akka Persistence Typed in Lagom requires tagging your events
        // in Lagom-compatible way so Lagom ReadSideProcessors and TopicProducers
        // can locate and follow the event streams.
        AkkaTaggerAdapter.fromLagom(entityContext, ExamregEvent.Tag)
      )

  }
  /*
   * This method is extracted to write unit tests that are completely independent to Akka Cluster.
   */
  private[impl] def create(persistenceId: PersistenceId): EventSourcedBehavior[ExamregCommand, ExamregEvent, ExamregState] = EventSourcedBehavior
    .withEnforcedReplies[ExamregCommand, ExamregEvent, ExamregState](
      persistenceId = persistenceId,
      emptyState = ExamregState.initial,
      commandHandler = (state, cmd) => state.applyCommand(cmd),
      eventHandler = (state, evt) => state.applyEvent(evt)
    )
}
