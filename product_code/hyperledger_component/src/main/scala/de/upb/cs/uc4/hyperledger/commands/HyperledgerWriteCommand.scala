package de.upb.cs.uc4.hyperledger.commands

import akka.actor.typed.ActorRef
import de.upb.cs.uc4.shared.server.messages.Confirmation

trait HyperledgerWriteCommand {
  val replyTo: ActorRef[Confirmation]
}
