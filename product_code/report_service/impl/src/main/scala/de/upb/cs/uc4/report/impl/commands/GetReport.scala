package de.upb.cs.uc4.report.impl.commands

import akka.actor.typed.ActorRef
import de.upb.cs.uc4.report.impl.actor.ReportWrapper

case class GetReport(replyTo: ActorRef[ReportWrapper]) extends ReportCommand
