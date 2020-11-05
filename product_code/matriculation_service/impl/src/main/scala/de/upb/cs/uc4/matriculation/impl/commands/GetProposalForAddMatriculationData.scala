package de.upb.cs.uc4.matriculation.impl.commands

import akka.actor.typed.ActorRef
import de.upb.cs.uc4.hyperledger.commands.HyperledgerProposalCommand
import de.upb.cs.uc4.matriculation.model.ImmatriculationData

import scala.util.Try

case class GetProposalForAddMatriculationData(data: ImmatriculationData, replyTo: ActorRef[Try[Array[Byte]]])
  extends HyperledgerProposalCommand
