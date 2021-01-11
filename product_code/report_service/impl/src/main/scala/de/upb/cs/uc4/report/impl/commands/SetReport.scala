package de.upb.cs.uc4.report.impl.commands

import akka.actor.typed.ActorRef
import de.upb.cs.uc4.report.impl.actor.TextReport
import de.upb.cs.uc4.shared.server.messages.Confirmation

case class SetReport(textReport: TextReport, profilePicture: Array[Byte], thumbnail: Array[Byte], timestamp: String, replyTo: ActorRef[Confirmation]) extends ReportCommand
