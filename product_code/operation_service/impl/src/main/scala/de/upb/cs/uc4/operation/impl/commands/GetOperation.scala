package de.upb.cs.uc4.operation.impl.commands

import akka.actor.typed.ActorRef
import akka.pattern.StatusReply
import de.upb.cs.uc4.hyperledger.commands.HyperledgerReadCommand
import de.upb.cs.uc4.operation.model.OperationData

case class GetOperation(replyTo: ActorRef[StatusReply[OperationData]]) extends HyperledgerReadCommand[OperationData]
