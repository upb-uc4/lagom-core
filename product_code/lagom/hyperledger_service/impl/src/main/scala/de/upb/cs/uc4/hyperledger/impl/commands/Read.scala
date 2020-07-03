package de.upb.cs.uc4.hyperledger.impl.commands

import akka.actor.typed.ActorRef

case class Read(key: String, replyTo: ActorRef[Option[String]]) extends HyperLedgerCommand
