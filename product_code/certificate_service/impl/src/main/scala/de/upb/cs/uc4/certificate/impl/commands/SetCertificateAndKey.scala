package de.upb.cs.uc4.certificate.impl.commands

import akka.actor.typed.ActorRef
import de.upb.cs.uc4.certificate.model.EncryptedPrivateKey
import de.upb.cs.uc4.shared.server.messages.Confirmation

case class SetCertificateAndKey(certificate: String, encryptedPrivateKey: EncryptedPrivateKey, replyTo: ActorRef[Confirmation]) extends CertificateCommand
