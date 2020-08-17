package de.upb.cs.uc4.user.impl.commands

import akka.actor.typed.ActorRef
import de.upb.cs.uc4.user.model.user.User

case class GetUser(replyTo: ActorRef[Option[User]]) extends UserCommand
