package de.upb.cs.uc4.matriculation.impl.commands

import akka.actor.typed.ActorRef
import akka.pattern.StatusReply
import de.upb.cs.uc4.hyperledger.commands.HyperledgerReadCommand
import de.upb.cs.uc4.matriculation.model.ImmatriculationData

case class GetMatriculationData(matriculationId: String, replyTo: ActorRef[StatusReply[ImmatriculationData]])
  extends HyperledgerReadCommand[ImmatriculationData]
