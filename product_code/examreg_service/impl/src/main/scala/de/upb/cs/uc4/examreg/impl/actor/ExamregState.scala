package de.upb.cs.uc4.examreg.impl.actor

import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import akka.persistence.typed.scaladsl.{ Effect, ReplyEffect }
import de.upb.cs.uc4.examreg.impl.ExamregApplication
import de.upb.cs.uc4.examreg.impl.commands.{ CreateExamreg, ExamregCommand, GetExamreg }
import de.upb.cs.uc4.examreg.impl.events.{ ExamregEvent, OnExamregCreate }
import de.upb.cs.uc4.examreg.model.ExaminationRegulation
import de.upb.cs.uc4.shared.server.messages.Accepted
import play.api.libs.json.{ Format, Json }

/** The current state of a ExaminationRegulation */
case class ExamregState(optExaminationRegulation: Option[ExaminationRegulation]) {

  /** Functions as a CommandHandler
    *
    * @param cmd the given command
    */
  def applyCommand(cmd: ExamregCommand): ReplyEffect[ExamregEvent, ExamregState] =
    cmd match {
      case GetExamreg(replyTo) =>
        Effect.reply(replyTo)(optExaminationRegulation)

      case CreateExamreg(examreg, replyTo) =>
        Effect.persist(OnExamregCreate(examreg)).thenReply(replyTo) { _ => Accepted }

      case _ =>
        println("Unknown Command")
        Effect.noReply
    }

  /** Functions as an EventHandler
    *
    * @param evt the given event
    */
  def applyEvent(evt: ExamregEvent): ExamregState =
    evt match {
      case OnExamregCreate(examreg) => copy(Some(examreg))
      case _ =>
        println("Unknown Event")
        this
    }
}

object ExamregState {

  /** The initial state. This is used if there is no snapshotted state to be found.
    */
  def initial: ExamregState = ExamregState(None)

  /** The [[akka.persistence.typed.scaladsl.EventSourcedBehavior]] instances (aka Aggregates) run on sharded actors inside the Akka Cluster.
    * When sharding actors and distributing them across the cluster, each aggregate is
    * namespaced under a typekey that specifies a name and also the type of the commands
    * that sharded actor can receive.
    */
  val typeKey: EntityTypeKey[ExamregCommand] = EntityTypeKey[ExamregCommand](ExamregApplication.offset)

  /** Format for the course state.
    *
    * Persisted entities get snapshotted every configured number of events. This
    * means the state gets stored to the database, so that when the aggregate gets
    * loaded, you don't need to replay all the events, just the ones since the
    * snapshot. Hence, a JSON format needs to be declared so that it can be
    * serialized and deserialized when storing to and from the database.
    */
  implicit val format: Format[ExamregState] = Json.format
}
