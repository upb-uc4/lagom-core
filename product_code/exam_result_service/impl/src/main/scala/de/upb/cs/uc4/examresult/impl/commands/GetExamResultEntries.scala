package de.upb.cs.uc4.examresult.impl.commands

import akka.actor.typed.ActorRef
import akka.pattern.StatusReply
import de.upb.cs.uc4.examresult.impl.actor.ExamResultWrapper
import de.upb.cs.uc4.hyperledger.impl.commands.HyperledgerReadCommand

case class GetExamResultEntries(enrollmentId: Option[String], examIds: Option[Seq[String]], replyTo: ActorRef[StatusReply[ExamResultWrapper]]) extends HyperledgerReadCommand[ExamResultWrapper]
