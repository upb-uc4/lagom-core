package de.upb.cs.uc4.operation.impl.commands

import akka.actor.typed.ActorRef
import akka.pattern.StatusReply
import de.upb.cs.uc4.hyperledger.impl.commands.HyperledgerReadCommand
import de.upb.cs.uc4.operation.impl.actor.OperationDataList

case class GetOperationsHyperledger(
    operationIds: Seq[String],
    existingEnrollmentId: String,
    missingEnrollmentId: String,
    initiatorEnrollmentId: String,
    involvedEnrollmentId: String,
    states: Seq[String],
    replyTo: ActorRef[StatusReply[OperationDataList]]
) extends HyperledgerReadCommand[OperationDataList]
