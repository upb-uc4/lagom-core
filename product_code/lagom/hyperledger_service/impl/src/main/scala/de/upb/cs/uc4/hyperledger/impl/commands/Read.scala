package de.upb.cs.uc4.hyperledger.impl.commands

import akka.actor.typed.ActorRef

case class Read(transactionId: String, params: Seq[String], replyTo: ActorRef[Option[String]]) extends HyperLedgerCommand
