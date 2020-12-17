package de.upb.cs.uc4.report.impl.actor

import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import akka.persistence.typed.scaladsl.{ Effect, ReplyEffect }
import de.upb.cs.uc4.report.impl.ReportApplication
import de.upb.cs.uc4.report.impl.actor.ReportState.initial
import de.upb.cs.uc4.report.impl.commands._
import de.upb.cs.uc4.report.impl.events.{ OnDeleteReport, OnPrepareReport, OnSetReport, ReportEvent }
import de.upb.cs.uc4.shared.client.exceptions.{ ErrorType, GenericError }
import de.upb.cs.uc4.shared.server.messages.{ Accepted, Rejected }
import play.api.libs.json.{ Format, Json }

/** The current state of a Report */
case class ReportState(reportWrapper: ReportWrapper) {

  /** Functions as a CommandHandler
    *
    * @param cmd the given command
    */
  def applyCommand(cmd: ReportCommand): ReplyEffect[ReportEvent, ReportState] =
    cmd match {
      case GetReport(replyTo) => Effect.reply(replyTo)(reportWrapper)
      case SetReport(report, replyTo) =>
        reportWrapper.state match {
          case ReportStateEnum.Ready     => Effect.reply(replyTo)(Rejected(500, GenericError(ErrorType.InternalServer, "Trying to set an already existing report.")))
          case ReportStateEnum.Preparing => Effect.persist(OnSetReport(report)).thenReply(replyTo) { _ => Accepted.default }
          case ReportStateEnum.None      => Effect.reply(replyTo)(Accepted("Report already exists."))
        }
      case DeleteReport(replyTo) =>
        reportWrapper.state match {
          case ReportStateEnum.Ready     => Effect.persist(OnDeleteReport(reportWrapper)).thenReply(replyTo) { _ => Accepted.default }
          case ReportStateEnum.Preparing => Effect.persist(OnDeleteReport(reportWrapper)).thenReply(replyTo) { _ => Accepted.default }
          case ReportStateEnum.None      => Effect.reply(replyTo)(Accepted("Report does not exist or was already deleted."))
        }
      case PrepareReport(timestamp, replyTo) =>
        reportWrapper.state match {
          case ReportStateEnum.Ready     => Effect.reply(replyTo)(Rejected(500, GenericError(ErrorType.InternalServer, "Trying to prepare an already existing report.")))
          case ReportStateEnum.Preparing => Effect.reply(replyTo)(Rejected(500, GenericError(ErrorType.InternalServer, "Trying to prepare an already preparing report.")))
          case ReportStateEnum.None      => Effect.persist(OnPrepareReport(timestamp)).thenReply(replyTo) { _ => Accepted.default }
        }
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
      case OnSetReport(report) =>
        copy(ReportWrapper(Some(report), reportWrapper.timestamp, ReportStateEnum.Ready))
      case OnDeleteReport(_) =>
        initial
      case OnPrepareReport(timestamp) =>
        copy(ReportWrapper(None, Some(timestamp), ReportStateEnum.Preparing))
      case _ =>
        println("Unknown Event")
        this
    }
}

object ReportState {

  /** The initial state. This is used if there is no snapshotted state to be found.
    */
  def initial: ReportState = ReportState(ReportWrapper(None, None, ReportStateEnum.None))

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
