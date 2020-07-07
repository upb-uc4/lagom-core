package de.upb.cs.uc4.user.impl.commands

import akka.actor.typed.ActorRef
import de.upb.cs.uc4.shared.server.messages.Confirmation
import de.upb.cs.uc4.user.model.user.{AuthenticationUser, User}

case class CreateUser(user: User, auth: AuthenticationUser, replyTo: ActorRef[Confirmation]) extends UserCommand
