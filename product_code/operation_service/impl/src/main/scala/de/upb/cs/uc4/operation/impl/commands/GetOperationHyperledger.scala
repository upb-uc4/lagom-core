package de.upb.cs.uc4.operation.impl.commands

import akka.actor.typed.ActorRef
import akka.pattern.StatusReply
import de.upb.cs.uc4.hyperledger.impl.commands.HyperledgerReadCommand
import de.upb.cs.uc4.shared.client.operation.OperationData

case class GetOperationHyperledger(operationId: String, replyTo: ActorRef[StatusReply[OperationData]]) extends HyperledgerReadCommand[OperationData]
