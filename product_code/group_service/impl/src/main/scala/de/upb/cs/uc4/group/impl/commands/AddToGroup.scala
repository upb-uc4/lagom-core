package de.upb.cs.uc4.group.impl.commands

import akka.actor.typed.ActorRef
import akka.pattern.StatusReply
import de.upb.cs.uc4.hyperledger.impl.commands.HyperledgerWriteCommand
import de.upb.cs.uc4.shared.server.messages.Confirmation

case class AddToGroup(enrollmentId: String, group: String, replyTo: ActorRef[StatusReply[Confirmation]]) extends HyperledgerWriteCommand
