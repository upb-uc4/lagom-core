package de.upb.cs.uc4.operation.impl.commands

import akka.actor.typed.ActorRef
import akka.pattern.StatusReply
import de.upb.cs.uc4.hyperledger.commands.HyperledgerReadCommand
import de.upb.cs.uc4.operation.impl.actor.OperationDataList

case class GetOperationsHyperledger(
    existingEnrollmentId: String,
    missingEnrollmentId: String,
    initiatorEnrollmentId: String,
    state: String,
    replyTo: ActorRef[StatusReply[OperationDataList]]
) extends HyperledgerReadCommand[OperationDataList]
