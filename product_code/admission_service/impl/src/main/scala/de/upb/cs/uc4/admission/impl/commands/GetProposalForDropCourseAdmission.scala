package de.upb.cs.uc4.admission.impl.commands

import akka.actor.typed.ActorRef
import akka.pattern.StatusReply
import de.upb.cs.uc4.admission.model.DropAdmission
import de.upb.cs.uc4.hyperledger.commands.HyperledgerProposalCommand

case class GetProposalForDropCourseAdmission(certificate: String, dropAdmission: DropAdmission, replyTo: ActorRef[StatusReply[Array[Byte]]]) extends HyperledgerProposalCommand

