package de.upb.cs.uc4.operation.impl.commands

import akka.actor.typed.ActorRef
import de.upb.cs.uc4.shared.server.messages.Confirmation

case class ClearWatchlist(username: String, replyTo: ActorRef[Confirmation]) extends OperationCommand
