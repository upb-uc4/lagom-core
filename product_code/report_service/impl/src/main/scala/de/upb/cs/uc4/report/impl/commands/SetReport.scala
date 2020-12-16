package de.upb.cs.uc4.report.impl.commands

import akka.actor.typed.ActorRef
import de.upb.cs.uc4.report.impl.actor.Report
import de.upb.cs.uc4.shared.server.messages.Confirmation

case class SetReport(report: Report, replyTo: ActorRef[Confirmation]) extends ReportCommand
