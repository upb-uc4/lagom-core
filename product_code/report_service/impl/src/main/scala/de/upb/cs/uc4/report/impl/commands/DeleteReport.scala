package de.upb.cs.uc4.report.impl.commands

import akka.actor.typed.ActorRef
import de.upb.cs.uc4.shared.server.messages.Confirmation

case class DeleteReport(replyTo: ActorRef[Confirmation]) extends ReportCommand
