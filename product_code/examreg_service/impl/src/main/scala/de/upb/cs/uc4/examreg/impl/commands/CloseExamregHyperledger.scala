package de.upb.cs.uc4.examreg.impl.commands

import akka.actor.typed.ActorRef
import akka.pattern.StatusReply
import de.upb.cs.uc4.hyperledger.commands.HyperledgerWriteCommand
import de.upb.cs.uc4.shared.server.messages.Confirmation

case class CloseExamregHyperledger(name: String, replyTo: ActorRef[StatusReply[Confirmation]]) extends HyperledgerWriteCommand
