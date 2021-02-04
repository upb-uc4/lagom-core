package de.upb.cs.uc4.exam.impl.commands

import akka.actor.typed.ActorRef
import akka.pattern.StatusReply
import de.upb.cs.uc4.exam.impl.actor.ExamsWrapper
import de.upb.cs.uc4.hyperledger.impl.commands.HyperledgerReadCommand

case class GetExams(
    examIds: Seq[String],
    courseIds: Seq[String],
    lecturerIds: Seq[String],
    moduleIds: Seq[String],
    types: Seq[String],
    admittableAt: Option[String],
    droppableAt: Option[String],
    replyTo: ActorRef[StatusReply[ExamsWrapper]]
)
  extends HyperledgerReadCommand[ExamsWrapper]