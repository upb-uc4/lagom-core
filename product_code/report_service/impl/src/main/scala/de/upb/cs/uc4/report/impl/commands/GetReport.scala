package de.upb.cs.uc4.report.impl.commands

import akka.actor.typed.ActorRef
import de.upb.cs.uc4.report.impl.actor.Report

case class GetReport(replyTo: ActorRef[Option[Report]]) extends ReportCommand
