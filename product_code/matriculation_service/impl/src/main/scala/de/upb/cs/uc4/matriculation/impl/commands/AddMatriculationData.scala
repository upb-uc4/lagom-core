package de.upb.cs.uc4.matriculation.impl.commands

import akka.actor.typed.ActorRef
import de.upb.cs.uc4.hyperledger.commands.{ HyperledgerCommand, HyperledgerWriteCommand }
import de.upb.cs.uc4.matriculation.model.ImmatriculationData
import de.upb.cs.uc4.shared.server.messages.Confirmation

case class AddMatriculationData(data: ImmatriculationData, replyTo: ActorRef[Confirmation])
  extends HyperledgerCommand with HyperledgerWriteCommand
