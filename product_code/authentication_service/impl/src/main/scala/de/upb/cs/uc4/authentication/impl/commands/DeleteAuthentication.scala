package de.upb.cs.uc4.authentication.impl.commands

import akka.actor.typed.ActorRef
import de.upb.cs.uc4.shared.server.messages.Confirmation

case class DeleteAuthentication(replyTo: ActorRef[Confirmation]) extends AuthenticationCommand
