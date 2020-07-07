package de.upb.cs.uc4.user.impl.commands

import akka.actor.typed.ActorRef
import de.upb.cs.uc4.shared.server.messages.Confirmation
import de.upb.cs.uc4.user.model.user.User

case class UpdateUser(user: User, replyTo: ActorRef[Confirmation]) extends UserCommand
