package de.upb.cs.uc4.authentication.impl.actor

import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import akka.persistence.typed.scaladsl.{Effect, ReplyEffect}
import de.upb.cs.uc4.authentication.impl.AuthenticationApplication
import de.upb.cs.uc4.authentication.impl.commands.{AuthenticationCommand, DeleteAuthentication, GetAuthentication, SetAuthentication}
import de.upb.cs.uc4.authentication.impl.events.{AuthenticationEvent, OnDelete, OnSet}
import de.upb.cs.uc4.shared.server.Hashing
import de.upb.cs.uc4.shared.server.messages.{Accepted, Rejected}
import play.api.libs.json.{Format, Json}

import scala.util.Random

/** The current state of an AuthenticationEntry */
case class AuthenticationState(optEntry: Option[AuthenticationEntry]) {

  /** Functions as a CommandHandler
   *
   * @param cmd the given command
   */
  def applyCommand(cmd: AuthenticationCommand): ReplyEffect[AuthenticationEvent, AuthenticationState] =
    cmd match {
      case SetAuthentication(user, replyTo) => Effect.persist(OnSet(user)).thenReply(replyTo) { _ => Accepted }
      case DeleteAuthentication(replyTo) => optEntry match {
        case Some(entry) =>
          Effect.persist(OnDelete(entry.username)).thenReply(replyTo) { _ => Accepted }
        case None =>
          Effect.reply(replyTo)(Rejected("AuthenticationUser does not exist."))
      }
      case GetAuthentication(replyTo) => Effect.reply(replyTo)(optEntry)
      case _ =>
        println("Unknown Command")
        Effect.noReply
    }


  /** Functions as an EventHandler
   *
   * @param evt the given event
   */
  def applyEvent(evt: AuthenticationEvent): AuthenticationState =
    evt match {
      case OnSet(user) =>
        val salt = Random.alphanumeric.take(64).mkString
        copy(Some(AuthenticationEntry(
          Hashing.sha256(user.username),
          salt,
          Hashing.sha256(salt + user.password),
          user.role
        )))
      case OnDelete(_) => copy(None)
      case _ =>
        println("Unknown Event")
        this
    }
}

object AuthenticationState {

  /**
   * The initial state. This is used if there is no snapshotted state to be found.
   */
  def initial: AuthenticationState = AuthenticationState(None)

  /**
   * The [[akka.persistence.typed.scaladsl.EventSourcedBehavior]] instances (aka Aggregates) run on sharded actors inside the Akka Cluster.
   * When sharding actors and distributing them across the cluster, each aggregate is
   * namespaced under a typekey that specifies a name and also the type of the commands
   * that sharded actor can receive.
   */
  val typeKey: EntityTypeKey[AuthenticationCommand] = EntityTypeKey[AuthenticationCommand](AuthenticationApplication.offset)

  /**
   * Format for the authentication state.
   *
   * Persisted entities get snapshotted every configured number of events. This
   * means the state gets stored to the database, so that when the aggregate gets
   * loaded, you don't need to replay all the events, just the ones since the
   * snapshot. Hence, a JSON format needs to be declared so that it can be
   * serialized and deserialized when storing to and from the database.
   */
  implicit val format: Format[AuthenticationState] = Json.format
}
