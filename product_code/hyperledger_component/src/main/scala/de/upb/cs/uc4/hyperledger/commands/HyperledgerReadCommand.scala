package de.upb.cs.uc4.hyperledger.commands

import akka.actor.typed.ActorRef

import scala.util.Try

trait HyperledgerReadCommand[PayloadType] {
  val replyTo: ActorRef[Try[PayloadType]]
}
