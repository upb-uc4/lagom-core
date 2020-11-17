package de.upb.cs.uc4.certificate.impl.commands

import akka.actor.typed.ActorRef
import de.upb.cs.uc4.certificate.impl.actor.CertificateUser

case class GetCertificateUser(replyTo: ActorRef[CertificateUser]) extends CertificateCommand
