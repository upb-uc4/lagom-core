package de.upb.cs.uc4.examreg.impl.commands

import akka.actor.typed.ActorRef
import akka.pattern.StatusReply
import de.upb.cs.uc4.examreg.model.ExaminationRegulationsWrapper
import de.upb.cs.uc4.hyperledger.impl.commands.HyperledgerReadCommand

case class GetAllExamregsHyperledger(replyTo: ActorRef[StatusReply[ExaminationRegulationsWrapper]]) extends HyperledgerReadCommand[ExaminationRegulationsWrapper]
