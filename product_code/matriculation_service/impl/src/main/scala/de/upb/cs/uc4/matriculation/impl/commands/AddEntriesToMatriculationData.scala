package de.upb.cs.uc4.matriculation.impl.commands

import akka.actor.typed.ActorRef
import de.upb.cs.uc4.hyperledger.commands.HyperledgerWriteCommand
import de.upb.cs.uc4.matriculation.model.SubjectMatriculation
import de.upb.cs.uc4.shared.server.messages.Confirmation

case class AddEntriesToMatriculationData(
    matriculationId: String,
    matriculation: Seq[SubjectMatriculation],
    replyTo: ActorRef[Confirmation]
)
  extends HyperledgerWriteCommand
