package de.upb.cs.uc4.admission.impl.commands

import akka.actor.typed.ActorRef
import akka.pattern.StatusReply
import de.upb.cs.uc4.admission.impl.actor.AdmissionsWrapper
import de.upb.cs.uc4.hyperledger.impl.commands.HyperledgerReadCommand

case class GetExamAdmissions(
    enrollmentId: Option[String],
    admissionIds: Option[Seq[String]],
    examIds: Option[Seq[String]],
    replyTo: ActorRef[StatusReply[AdmissionsWrapper]]
)
  extends HyperledgerReadCommand[AdmissionsWrapper]
