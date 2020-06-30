package de.upb.cs.uc4.user.impl.commands

import akka.actor.typed.ActorRef
import de.upb.cs.uc4.shared.messages.Confirmation
import de.upb.cs.uc4.user.impl.actor.User
import de.upb.cs.uc4.user.model.user.AuthenticationUser

case class CreateUser(user: User, auth: AuthenticationUser, replyTo: ActorRef[Confirmation]) extends UserCommand
