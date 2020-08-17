package de.upb.cs.uc4.authentication.impl.commands

import akka.actor.typed.ActorRef
import de.upb.cs.uc4.authentication.impl.actor.AuthenticationEntry

case class GetAuthentication(replyTo: ActorRef[Option[AuthenticationEntry]]) extends AuthenticationCommand
