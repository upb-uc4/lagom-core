package de.upb.cs.uc4.report.impl

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.persistence.typed.PersistenceId
import de.upb.cs.uc4.report.impl.actor.{ ReportBehaviour, ReportStateEnum, ReportWrapper, TextReport }
import de.upb.cs.uc4.report.impl.commands.{ DeleteReport, GetReport, PrepareReport, SetReport }
import de.upb.cs.uc4.shared.server.messages.{ Accepted, Confirmation, Rejected }
import de.upb.cs.uc4.user.DefaultTestUsers
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID

case class ReportStateSpec() extends ScalaTestWithActorTestKit(
  s"""
      akka.persistence.journal.plugin = "akka.persistence.journal.inmem"
      akka.persistence.snapshot-store.plugin = "akka.persistence.snapshot-store.local"
      akka.persistence.snapshot-store.local.dir = "target/snapshot-${UUID.randomUUID().toString}"
    """
) with AnyWordSpecLike with Matchers with DefaultTestUsers {

  "ReportState" should {

    "get not prepared report" in {
      val probe = createTestProbe[ReportWrapper]()
      val ref = spawn(ReportBehaviour.create(PersistenceId("fake-type-hint", "fake-id-1")))
      ref ! GetReport(probe.ref)
      probe.expectMessage(ReportWrapper(None, None, None, None, ReportStateEnum.None))
    }

    "prepare a report" in {
      val ref = spawn(ReportBehaviour.create(PersistenceId("fake-type-hint", "fake-id-2")))

      val timestamp = "2020-12-14"

      val probe1 = createTestProbe[Confirmation]()
      ref ! PrepareReport(timestamp, probe1.ref)
      probe1.expectMessageType[Accepted]

      val probe2 = createTestProbe[ReportWrapper]()
      ref ! GetReport(probe2.ref)
      probe2.expectMessage(ReportWrapper(None, None, None, Some(timestamp), ReportStateEnum.Preparing))
    }

    "set a report" in {
      val ref = spawn(ReportBehaviour.create(PersistenceId("fake-type-hint", "fake-id-3")))

      val timestamp = "2020-12-14"
      val testReport = TextReport(student0, None, student0.username + "enrollmentID", None, None, None, None, Seq(), Seq(), timestamp)

      // Prepare report
      val probe1 = createTestProbe[Confirmation]()
      ref ! PrepareReport(timestamp, probe1.ref)
      probe1.expectMessageType[Accepted]

      // Set report (preparing => ready)
      val probe2 = createTestProbe[Confirmation]()
      ref ! SetReport(testReport, Array.emptyByteArray, Array.emptyByteArray, timestamp, probe2.ref)
      probe2.expectMessageType[Accepted]

      // Fetch ready report
      val probe3 = createTestProbe[ReportWrapper]()
      ref ! GetReport(probe3.ref)
      probe3.expectMessage(ReportWrapper(Some(testReport), Some(Array.emptyByteArray), Some(Array.emptyByteArray), Some(timestamp), ReportStateEnum.Ready))
    }

    "delete a report" in {
      val ref = spawn(ReportBehaviour.create(PersistenceId("fake-type-hint", "fake-id-4")))

      val timestamp = "2020-12-14"
      val testReport = TextReport(student0, None, student0.username + "enrollmentID", None, None, None, None, Seq(), Seq(), timestamp)

      // Prepare
      val probe1 = createTestProbe[Confirmation]()
      ref ! PrepareReport(timestamp, probe1.ref)
      probe1.expectMessageType[Accepted]

      // Set report (preparing => ready)
      val probe2 = createTestProbe[Confirmation]()
      ref ! SetReport(testReport, Array.emptyByteArray, Array.emptyByteArray, timestamp, probe2.ref)
      probe2.expectMessageType[Accepted]

      // Fetch ready report
      val probe3 = createTestProbe[ReportWrapper]()
      ref ! GetReport(probe3.ref)
      probe3.expectMessage(ReportWrapper(Some(testReport), Some(Array.emptyByteArray), Some(Array.emptyByteArray), Some(timestamp), ReportStateEnum.Ready))

      // Delete Report
      val probe4 = createTestProbe[Confirmation]()
      ref ! DeleteReport(probe4.ref)
      probe4.expectMessageType[Accepted]

      val probe5 = createTestProbe[ReportWrapper]()
      ref ! GetReport(probe5.ref)
      probe5.expectMessage(ReportWrapper(None, None, None, None, ReportStateEnum.None))
    }

    "not fail on trying to delete a non-existing report" in {
      val ref = spawn(ReportBehaviour.create(PersistenceId("fake-type-hint", "fake-id-5")))

      val probe1 = createTestProbe[Confirmation]()
      ref ! DeleteReport(probe1.ref)
      probe1.expectMessageType[Accepted]
    }

    "not prepare a report twice" in {
      val ref = spawn(ReportBehaviour.create(PersistenceId("fake-type-hint", "fake-id-6")))

      val timestamp = "2020-12-14"

      val probe1 = createTestProbe[Confirmation]()
      ref ! PrepareReport(timestamp, probe1.ref)
      probe1.expectMessageType[Accepted]

      val probe2 = createTestProbe[Confirmation]()
      ref ! PrepareReport(timestamp, probe2.ref)
      probe2.expectMessageType[Rejected]

      val probe3 = createTestProbe[ReportWrapper]()
      ref ! GetReport(probe3.ref)
      probe3.expectMessage(ReportWrapper(None, None, None, Some(timestamp), ReportStateEnum.Preparing))
    }

    "not prepare a report that is in ready state" in {
      val ref = spawn(ReportBehaviour.create(PersistenceId("fake-type-hint", "fake-id-7")))

      val timestamp = "2020-12-14"
      val testReport = TextReport(student0, None, student0.username + "enrollmentID", None, None, None, None, Seq(), Seq(), timestamp)

      // Prepare report
      val probe1 = createTestProbe[Confirmation]()
      ref ! PrepareReport(timestamp, probe1.ref)
      probe1.expectMessageType[Accepted]

      // Set report (preparing => ready)
      val probe2 = createTestProbe[Confirmation]()
      ref ! SetReport(testReport, Array.emptyByteArray, Array.emptyByteArray, timestamp, probe2.ref)
      probe2.expectMessageType[Accepted]

      // Prepare report again
      val probe3 = createTestProbe[Confirmation]()
      ref ! PrepareReport(timestamp, probe3.ref)
      probe3.expectMessageType[Rejected]
    }

    "delete a report in preparing" in {
      val ref = spawn(ReportBehaviour.create(PersistenceId("fake-type-hint", "fake-id-8")))

      val timestamp = "2020-12-14"

      // Prepare
      val probe1 = createTestProbe[Confirmation]()
      ref ! PrepareReport(timestamp, probe1.ref)
      probe1.expectMessageType[Accepted]

      // Delete Report
      val probe2 = createTestProbe[Confirmation]()
      ref ! DeleteReport(probe2.ref)
      probe2.expectMessageType[Accepted]

      // Fetch preparing report
      val probe3 = createTestProbe[ReportWrapper]()
      ref ! GetReport(probe3.ref)
      probe3.expectMessage(ReportWrapper(None, None, None, None, ReportStateEnum.None))
    }

    "delete a report in preparing and not be set afterwards" in {
      val ref = spawn(ReportBehaviour.create(PersistenceId("fake-type-hint", "fake-id-8")))

      val timestamp = "2020-12-14"
      val testReport = TextReport(student0, None, student0.username + "enrollmentID", None, None, None, None, Seq(), Seq(), timestamp)

      // Prepare
      val probe1 = createTestProbe[Confirmation]()
      ref ! PrepareReport(timestamp, probe1.ref)
      probe1.expectMessageType[Accepted]

      // Delete Report
      val probe2 = createTestProbe[Confirmation]()
      ref ! DeleteReport(probe2.ref)
      probe2.expectMessageType[Accepted]

      // Set report (preparing => ready)
      val probe3 = createTestProbe[Confirmation]()
      ref ! SetReport(testReport, Array.emptyByteArray, Array.emptyByteArray, timestamp, probe3.ref)
      probe3.expectMessageType[Accepted]

      // Fetch preparing report
      val probe4 = createTestProbe[ReportWrapper]()
      ref ! GetReport(probe4.ref)
      probe4.expectMessage(ReportWrapper(None, None, None, None, ReportStateEnum.None))

    }

    "delete a report in preparing and prepare another report, without the older prepared report being applied" in {
      val ref = spawn(ReportBehaviour.create(PersistenceId("fake-type-hint", "fake-id-8")))

      val timestampOld = "2020-12-14"
      val testReport = TextReport(student0, None, student0.username + "enrollmentID", None, None, None, None, Seq(), Seq(), timestampOld)
      val timestampNew = "2020-12-15"

      // Prepare
      val probe1 = createTestProbe[Confirmation]()
      ref ! PrepareReport(timestampOld, probe1.ref)
      probe1.expectMessageType[Accepted]

      // Delete Report
      val probe2 = createTestProbe[Confirmation]()
      ref ! DeleteReport(probe2.ref)
      probe2.expectMessageType[Accepted]

      // Prepare
      val probe3 = createTestProbe[Confirmation]()
      ref ! PrepareReport(timestampNew, probe3.ref)
      probe3.expectMessageType[Accepted]

      // Set report (preparing => ready)
      val probe4 = createTestProbe[Confirmation]()
      ref ! SetReport(testReport, Array.emptyByteArray, Array.emptyByteArray, timestampOld, probe4.ref)
      probe4.expectMessageType[Accepted]

      // Fetch preparing report
      val probe5 = createTestProbe[ReportWrapper]()
      ref ! GetReport(probe5.ref)
      probe5.expectMessage(ReportWrapper(None, None, None, Some(timestampNew), ReportStateEnum.Preparing))

    }
  }
}
