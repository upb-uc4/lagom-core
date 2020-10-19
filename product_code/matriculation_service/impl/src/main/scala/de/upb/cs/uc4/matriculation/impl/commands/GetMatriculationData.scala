package de.upb.cs.uc4.matriculation.impl.commands

import akka.actor.typed.ActorRef
import de.upb.cs.uc4.hyperledger.commands.HyperledgerReadCommand
import de.upb.cs.uc4.matriculation.model.ImmatriculationData

import scala.util.Try

case class GetMatriculationData(matriculationId: String, replyTo: ActorRef[Try[ImmatriculationData]])
  extends HyperledgerReadCommand[ImmatriculationData]
