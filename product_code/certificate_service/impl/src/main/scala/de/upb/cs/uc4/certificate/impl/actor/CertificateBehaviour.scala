package de.upb.cs.uc4.certificate.impl.actor

import akka.actor.typed.Behavior
import akka.cluster.sharding.typed.scaladsl.EntityContext
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.EventSourcedBehavior
import com.lightbend.lagom.scaladsl.persistence.AkkaTaggerAdapter
import com.typesafe.config.Config
import de.upb.cs.uc4.certificate.impl.commands.CertificateCommand
import de.upb.cs.uc4.certificate.impl.events.CertificateEvent

object CertificateBehaviour {

  /** Given a sharding [[EntityContext]] this function produces an Akka [[Behavior]] for the aggregate.
    */
  def create(entityContext: EntityContext[CertificateCommand])(implicit config: Config): Behavior[CertificateCommand] = {
    val persistenceId: PersistenceId = PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId)

    create(persistenceId)
      .withTagger(
        // Using Akka Persistence Typed in Lagom requires tagging your events
        // in Lagom-compatible way so Lagom ReadSideProcessors and TopicProducers
        // can locate and follow the event streams.
        AkkaTaggerAdapter.fromLagom(entityContext, CertificateEvent.Tag)
      )

  }
  /*
   * This method is extracted to write unit tests that are completely independent to Akka Cluster.
   */
  private[impl] def create(persistenceId: PersistenceId)(implicit config: Config): EventSourcedBehavior[CertificateCommand, CertificateEvent, CertificateState] = EventSourcedBehavior
    .withEnforcedReplies[CertificateCommand, CertificateEvent, CertificateState](
      persistenceId = persistenceId,
      emptyState = CertificateState.initial,
      commandHandler = (state, cmd) => state.applyCommand(cmd),
      eventHandler = (state, evt) => state.applyEvent(evt)
    )
}
