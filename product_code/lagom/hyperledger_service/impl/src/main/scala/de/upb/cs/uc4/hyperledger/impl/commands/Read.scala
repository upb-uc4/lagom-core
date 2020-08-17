package de.upb.cs.uc4.hyperledger.impl.commands

import akka.actor.typed.ActorRef

import scala.util.Try

case class Read(transactionId: String, params: Seq[String], replyTo: ActorRef[Try[String]]) extends HyperLedgerCommand
