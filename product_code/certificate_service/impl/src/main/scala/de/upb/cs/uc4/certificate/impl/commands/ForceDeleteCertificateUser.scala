package de.upb.cs.uc4.certificate.impl.commands

import akka.actor.typed.ActorRef
import de.upb.cs.uc4.shared.server.messages.Confirmation
import de.upb.cs.uc4.user.model.Role.Role

case class ForceDeleteCertificateUser(username: String, role: Role, replyTo: ActorRef[Confirmation]) extends CertificateCommand
