package de.upb.cs.uc4.matriculation.impl.commands

import akka.actor.typed.ActorRef
import de.upb.cs.uc4.hyperledger.commands.HyperledgerProposalCommand
import de.upb.cs.uc4.matriculation.model.SubjectMatriculation

import scala.util.Try

case class GetProposalForAddEntriesToMatriculationData(
    enrollmentId: String,
    matriculation: Seq[SubjectMatriculation],
    replyTo: ActorRef[Try[Array[Byte]]]
)
  extends HyperledgerProposalCommand
