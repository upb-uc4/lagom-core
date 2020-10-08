package de.upb.cs.uc4.certificate.impl.actor

import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import akka.persistence.typed.scaladsl.{ Effect, ReplyEffect }
import com.typesafe.config.Config
import de.upb.cs.uc4.certificate.impl.CertificateApplication
import de.upb.cs.uc4.certificate.impl.commands.{ CertificateCommand, GetCertificateUser, RegisterUser, SetCertificateAndKey }
import de.upb.cs.uc4.certificate.impl.events.{ CertificateEvent, OnCertficateAndKeySet, OnRegisterUser }
import de.upb.cs.uc4.certificate.model.EncryptedPrivateKey
import de.upb.cs.uc4.shared.server.messages.Accepted
import play.api.libs.json.{ Format, Json }

/** The current state of a User */
case class CertificateState(
    enrollmentId: Option[String],
    enrollmentSecret: Option[String],
    certificate: Option[String],
    encryptedPrivateKey: Option[EncryptedPrivateKey]
) {

  /** Functions as a CommandHandler
    *
    * @param cmd the given command
    */
  def applyCommand(cmd: CertificateCommand): ReplyEffect[CertificateEvent, CertificateState] =
    cmd match {
      case RegisterUser(enrollmentId, secret, replyTo) =>
        Effect.persist(OnRegisterUser(enrollmentId, secret)).thenReply(replyTo) { _ => Accepted }
      case GetCertificateUser(replyTo) =>
        Effect.reply(replyTo)(enrollmentId, enrollmentSecret, certificate, encryptedPrivateKey)
      case SetCertificateAndKey(certificate, encryptedPrivateKey, replyTo) =>
        Effect.persist(OnCertficateAndKeySet(certificate, encryptedPrivateKey)).thenReply(replyTo) { _ => Accepted }
      case _ =>
        println("Unknown Command")
        Effect.noReply
    }

  /** Functions as an EventHandler
    *
    * @param evt the given event
    */
  def applyEvent(evt: CertificateEvent): CertificateState =
    evt match {
      case OnRegisterUser(id, secret) =>
        copy(enrollmentId = Some(id), enrollmentSecret = Some(secret))
      case OnCertficateAndKeySet(cert, key) =>
        copy(certificate = Some(cert), encryptedPrivateKey = Some(key))
      case _ =>
        println("Unknown Event")
        this
    }
}

object CertificateState {

  /** The initial state. This is used if there is no snapshotted state to be found.
    */
  def initial: CertificateState = CertificateState(None, None, None, None)

  /** The [[akka.persistence.typed.scaladsl.EventSourcedBehavior]] instances (aka Aggregates) run on sharded actors inside the Akka Cluster.
    * When sharding actors and distributing them across the cluster, each aggregate is
    * namespaced under a typekey that specifies a name and also the type of the commands
    * that sharded actor can receive.
    */
  val typeKey: EntityTypeKey[CertificateCommand] = EntityTypeKey[CertificateCommand](CertificateApplication.offset)

  /** Format for the certificate state.
    *
    * Persisted entities get snapshotted every configured number of events. This
    * means the state gets stored to the database, so that when the aggregate gets
    * loaded, you don't need to replay all the events, just the ones since the
    * snapshot. Hence, a JSON format needs to be declared so that it can be
    * serialized and deserialized when storing to and from the database.
    */
  implicit def format: Format[CertificateState] = Json.format
}
