package de.upb.cs.uc4.operation.impl.commands

import akka.actor.typed.ActorRef
import de.upb.cs.uc4.operation.impl.actor.WatchlistWrapper

case class GetWatchlist(replyTo: ActorRef[WatchlistWrapper]) extends OperationCommand
