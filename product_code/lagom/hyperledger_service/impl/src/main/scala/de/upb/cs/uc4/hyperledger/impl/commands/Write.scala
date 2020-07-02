package de.upb.cs.uc4.hyperledger.impl.commands

import akka.actor.typed.ActorRef
import de.upb.cs.uc4.shared.messages.Confirmation

case class Write(json: String, replyTo: ActorRef[Confirmation]) extends HyperLedgerCommand
