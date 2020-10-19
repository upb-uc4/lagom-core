package de.upb.cs.uc4.user.impl.actor

import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import akka.persistence.typed.scaladsl.{ Effect, ReplyEffect }
import de.upb.cs.uc4.shared.client.Utils.SemesterUtils
import de.upb.cs.uc4.shared.client.exceptions.{ ErrorType, GenericError }
import de.upb.cs.uc4.shared.server.messages._
import de.upb.cs.uc4.user.impl.UserApplication
import de.upb.cs.uc4.user.impl.commands._
import de.upb.cs.uc4.user.impl.events._
import de.upb.cs.uc4.user.model.Role
import de.upb.cs.uc4.user.model.user.{ Student, User }
import play.api.libs.json.{ Format, Json }

/** The current state of a User */
case class UserState(optUser: Option[User]) {

  /** Functions as a CommandHandler
    *
    * @param cmd the given command
    */
  def applyCommand(cmd: UserCommand): ReplyEffect[UserEvent, UserState] =
    cmd match {
      case GetUser(replyTo) => Effect.reply(replyTo)(optUser)

      case CreateUser(user, replyTo) =>
        if (optUser.isEmpty) {
          Effect.persist(OnUserCreate(user)).thenReply(replyTo) { _ => Accepted }
        }
        else {
          Effect.reply(replyTo)(RejectedWithError(500, GenericError(ErrorType.InternalServer)))
        }

      case UpdateUser(user, replyTo) =>
        if (optUser.isDefined) {
          Effect.persist(OnUserUpdate(user)).thenReply(replyTo) { _ => Accepted }
        }
        else {
          Effect.reply(replyTo)(RejectedWithError(500, GenericError(ErrorType.InternalServer)))
        }

      case UpdateLatestMatriculation(semester, replyTo) =>
        //Check for existence and check for student
        if (optUser.isDefined && optUser.get.role == Role.Student) {
          if (semester.compareSemester(optUser.get.asInstanceOf[Student].latestImmatriculation) > 0) {
            Effect.persist(OnLatestMatriculationUpdate(semester)).thenReply(replyTo) { _ => Accepted }
          }
          else {
            Effect.reply(replyTo)(Accepted)
          }
        }
        else {
          Effect.reply(replyTo)(RejectedWithError(500, GenericError(ErrorType.InternalServer)))
        }

      case DeleteUser(replyTo) =>
        if (optUser.isDefined) {
          Effect.persist(OnUserDelete(optUser.get)).thenReply(replyTo) { _ => Accepted }
        }
        else {
          Effect.reply(replyTo)(RejectedWithError(404, GenericError(ErrorType.KeyNotFound, "A user with the given username does not exist.")))
        }

      case _ =>
        println("Unknown Command")
        Effect.noReply
    }

  /** Functions as an EventHandler
    *
    * @param evt the given event
    */
  def applyEvent(evt: UserEvent): UserState =
    evt match {
      case OnUserCreate(user) =>
        copy(Some(user))
      case OnUserUpdate(user) =>
        copy(Some(user))
      case OnLatestMatriculationUpdate(semester) =>
        copy(optUser.map {
          case student: Student => student.copy(latestImmatriculation = semester)
        })
      case OnUserDelete(_) =>
        copy(None)
      case _ =>
        println("Unknown Event")
        this
    }
}

object UserState {

  /** The initial state. This is used if there is no snapshotted state to be found.
    */
  def initial: UserState = UserState(None)

  /** The [[akka.persistence.typed.scaladsl.EventSourcedBehavior]] instances (aka Aggregates) run on sharded actors inside the Akka Cluster.
    * When sharding actors and distributing them across the cluster, each aggregate is
    * namespaced under a typekey that specifies a name and also the type of the commands
    * that sharded actor can receive.
    */
  val typeKey: EntityTypeKey[UserCommand] = EntityTypeKey[UserCommand](UserApplication.offset)

  /** Format for the course state.
    *
    * Persisted entities get snapshotted every configured number of events. This
    * means the state gets stored to the database, so that when the aggregate gets
    * loaded, you don't need to replay all the events, just the ones since the
    * snapshot. Hence, a JSON format needs to be declared so that it can be
    * serialized and deserialized when storing to and from the database.
    */
  implicit val format: Format[UserState] = Json.format
}
