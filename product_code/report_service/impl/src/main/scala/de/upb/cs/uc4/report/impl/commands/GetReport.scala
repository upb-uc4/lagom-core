package de.upb.cs.uc4.report.impl.commands

import akka.actor.typed.ActorRef

case class GetReport(replyTo: ActorRef[Option[Array[Byte]]]) extends ReportCommand
