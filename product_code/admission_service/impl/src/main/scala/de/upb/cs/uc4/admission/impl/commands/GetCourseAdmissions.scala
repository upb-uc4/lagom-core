package de.upb.cs.uc4.admission.impl.commands

import akka.actor.typed.ActorRef
import akka.pattern.StatusReply
import de.upb.cs.uc4.admission.impl.actor.AdmissionsWrapper
import de.upb.cs.uc4.hyperledger.commands.HyperledgerReadCommand

case class GetCourseAdmissions(
    enrollmentId: Option[String],
    courseId: Option[String],
    moduleId: Option[String],
    replyTo: ActorRef[StatusReply[AdmissionsWrapper]]
)
  extends HyperledgerReadCommand[AdmissionsWrapper]
