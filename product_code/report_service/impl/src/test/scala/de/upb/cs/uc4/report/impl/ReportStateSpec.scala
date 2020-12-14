package de.upb.cs.uc4.report.impl

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.persistence.typed.PersistenceId
import de.upb.cs.uc4.report.impl.actor.{ Report, ReportBehaviour }
import de.upb.cs.uc4.report.impl.commands.{ DeleteReport, GetReport, SetReport }
import de.upb.cs.uc4.shared.server.messages.{ Accepted, Confirmation }
import de.upb.cs.uc4.user.DefaultTestUsers
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID

case class ReportStateSpec() extends ScalaTestWithActorTestKit(
  s"""
      akka.persistence.journal.plugin = "akka.persistence.journal.inmem"
      akka.persistence.snapshot-store.plugin = "akka.persistence.snapshot-store.local"
      akka.persistence.snapshot-store.local.dir = "target/snapshot-${UUID.randomUUID().toString}"
    """) with AnyWordSpecLike with Matchers with DefaultTestUsers {

  "ReportState" should {

    "get empty report" in {
      val probe = createTestProbe[Option[Report]]()
      val ref = spawn(ReportBehaviour.create(PersistenceId("fake-type-hint", "fake-id-1")))
      ref ! GetReport(probe.ref)
      probe.expectMessage(None)
    }

    "set a report" in {
      val ref = spawn(ReportBehaviour.create(PersistenceId("fake-type-hint", "fake-id-2")))

      val testReport = Report(student0, None, student0.username + "enrollmentID", None, None, None, "2020-12-14")

      val probe1 = createTestProbe[Confirmation]()
      ref ! SetReport(testReport, probe1.ref)
      probe1.expectMessageType[Accepted]

      val probe2 = createTestProbe[Option[Report]]()
      ref ! GetReport(probe2.ref)
      probe2.expectMessage(Some(testReport))
    }

    "delete a report" in {
      val ref = spawn(ReportBehaviour.create(PersistenceId("fake-type-hint", "fake-id-3")))

      val testReport = Report(student0, None, student0.username + "enrollmentID", None, None, None, "2020-12-14")

      val probe1 = createTestProbe[Confirmation]()
      ref ! SetReport(testReport, probe1.ref)
      probe1.expectMessageType[Accepted]

      val probe2 = createTestProbe[Option[Report]]()
      ref ! GetReport(probe2.ref)
      probe2.expectMessage(Some(testReport))

      val probe3 = createTestProbe[Confirmation]()
      ref ! DeleteReport(probe3.ref)
      probe3.expectMessageType[Accepted]

      val probe4 = createTestProbe[Option[Report]]
      ref ! GetReport(probe4.ref)
      probe4.expectMessage(None)
    }

    "not fail on trying to delete a non-existing report" in {
      val ref = spawn(ReportBehaviour.create(PersistenceId("fake-type-hint", "fake-id-4")))

      val probe1 = createTestProbe[Confirmation]()
      ref ! DeleteReport(probe1.ref)
      probe1.expectMessageType[Accepted]
    }

  }
}
