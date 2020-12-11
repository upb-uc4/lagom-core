package de.upb.cs.uc4.report.impl.actor

import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import akka.persistence.typed.scaladsl.{ Effect, ReplyEffect }
import de.upb.cs.uc4.report.impl.ReportApplication
import de.upb.cs.uc4.report.impl.commands.ReportCommand
import de.upb.cs.uc4.report.impl.events.ReportEvent
import de.upb.cs.uc4.shared.client.exceptions.{ ErrorType, GenericError }
import de.upb.cs.uc4.shared.server.messages.{ Accepted, Rejected }
import play.api.libs.json.{ Format, Json }

/** The current state of a Report */
case class ReportState(changeMe: String) {

  /** Functions as a CommandHandler
    *
    * @param cmd the given command
    */
  def applyCommand(cmd: ReportCommand): ReplyEffect[ReportEvent, ReportState] =
    cmd match {
      case _ =>
        println("Unknown Command")
        Effect.noReply
    }

  /** Functions as an EventHandler
    *
    * @param evt the given event
    */
  def applyEvent(evt: ReportEvent): ReportState =
    evt match {
      case _ =>
        println("Unknown Event")
        this
    }
}

object ReportState {

  /** The initial state. This is used if there is no snapshotted state to be found.
    */
  def initial: ReportState = ReportState("")

  /** The [[akka.persistence.typed.scaladsl.EventSourcedBehavior]] instances (aka Aggregates) run on sharded actors inside the Akka Cluster.
    * When sharding actors and distributing them across the cluster, each aggregate is
    * namespaced under a typekey that specifies a name and also the type of the commands
    * that sharded actor can receive.
    */
  val typeKey: EntityTypeKey[ReportCommand] = EntityTypeKey[ReportCommand](ReportApplication.offset)

  /** Format for the course state.
    *
    * Persisted entities get snapshotted every configured number of events. This
    * means the state gets stored to the database, so that when the aggregate gets
    * loaded, you don't need to replay all the events, just the ones since the
    * snapshot. Hence, a JSON format needs to be declared so that it can be
    * serialized and deserialized when storing to and from the database.
    */
  implicit val format: Format[ReportState] = Json.format
}
