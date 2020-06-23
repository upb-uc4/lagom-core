package de.upb.cs.uc4.user.impl.commands

import akka.actor.typed.ActorRef
import de.upb.cs.uc4.shared.messages.Confirmation
import de.upb.cs.uc4.user.impl.actor.User

case class CreateUser(user: User, replyTo: ActorRef[Confirmation]) extends UserCommand
