package de.upb.cs.uc4.certificate.impl

import com.lightbend.lagom.scaladsl.playjson.JsonSerializer
import de.upb.cs.uc4.certificate.impl.actor.{ CertificateState, CertificateUser }
import de.upb.cs.uc4.certificate.impl.events.{ OnCertficateAndKeySet, OnCertificateUserDelete, OnRegisterUser }
import de.upb.cs.uc4.certificate.model.EncryptedPrivateKey
import de.upb.cs.uc4.shared.server.SharedSerializerRegistry

import scala.collection.immutable.Seq

/** Akka serialization, used by both persistence and remoting, needs to have
  * serializers registered for every type serialized or deserialized. While it's
  * possible to use any serializer you want for Akka messages, out of the box
  * Lagom provides support for JSON, via this registry abstraction.
  *
  * The serializers are registered here, and then provided to Lagom in the
  * application loader.
  */
object CertificateSerializerRegistry extends SharedSerializerRegistry {
  override def customSerializers: Seq[JsonSerializer[_]] = Seq(
    JsonSerializer[OnRegisterUser],
    JsonSerializer[OnCertficateAndKeySet],
    JsonSerializer[OnCertificateUserDelete],
    JsonSerializer[CertificateState],
    JsonSerializer[EncryptedPrivateKey],
    JsonSerializer[CertificateUser]
  )
}
