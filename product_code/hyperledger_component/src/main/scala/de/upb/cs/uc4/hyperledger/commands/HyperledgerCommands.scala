package de.upb.cs.uc4.hyperledger.commands

import akka.actor.typed.ActorRef
import de.upb.cs.uc4.shared.server.messages.Confirmation

import scala.util.Try

/** The trait for the commands needed in the state
  * Every command is a case class containing the
  * necessary information to execute the command
  */
sealed trait HyperledgerCommand extends HyperledgerCommandSerializable

trait HyperledgerReadCommand[PayloadType] extends HyperledgerCommand {
  val replyTo: ActorRef[Try[PayloadType]]
}

trait HyperledgerWriteCommand extends HyperledgerCommand {
  val replyTo: ActorRef[Confirmation]
}

case class Shutdown() extends HyperledgerCommand
