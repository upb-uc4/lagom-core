package de.upb.cs.uc4.certificate.impl.commands

import akka.actor.typed.ActorRef
import de.upb.cs.uc4.certificate.model.EncryptedPrivateKey

case class GetCertificateUser(replyTo: ActorRef[(
  Option[String],
    Option[String],
    Option[String],
    Option[EncryptedPrivateKey])
]) extends CertificateCommand
