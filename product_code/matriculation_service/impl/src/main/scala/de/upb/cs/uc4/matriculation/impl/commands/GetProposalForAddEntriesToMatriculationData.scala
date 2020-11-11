package de.upb.cs.uc4.matriculation.impl.commands

import akka.actor.typed.ActorRef
import akka.pattern.StatusReply
import de.upb.cs.uc4.hyperledger.commands.HyperledgerProposalCommand
import de.upb.cs.uc4.matriculation.model.SubjectMatriculation

case class GetProposalForAddEntriesToMatriculationData(
    enrollmentId: String,
    matriculation: Seq[SubjectMatriculation],
    replyTo: ActorRef[StatusReply[Array[Byte]]]
)
  extends HyperledgerProposalCommand
