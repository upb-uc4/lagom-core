package de.upb.cs.uc4.operation.impl

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.persistence.typed.PersistenceId
import de.upb.cs.uc4.operation.impl.actor.{ OperationDatabaseBehaviour, WatchlistWrapper }
import de.upb.cs.uc4.operation.impl.commands.{ AddToWatchlist, ClearWatchlist, GetWatchlist, RemoveFromWatchlist }
import de.upb.cs.uc4.shared.server.messages.{ Accepted, Confirmation }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID

/** Tests for the OperationState */
class OperationStateSpec extends ScalaTestWithActorTestKit(s"""
      akka.persistence.journal.plugin = "akka.persistence.journal.inmem"
      akka.persistence.snapshot-store.plugin = "akka.persistence.snapshot-store.local"
      akka.persistence.snapshot-store.local.dir = "target/snapshot-${UUID.randomUUID().toString}"
    """) with AnyWordSpecLike with Matchers {

  "OperationState" should {

    //GET
    "get the initial empty watchlist" in {
      val ref = spawn(OperationDatabaseBehaviour.create(PersistenceId("fake-type-hint", "fake-id-1")))
      val probe = createTestProbe[WatchlistWrapper]()
      ref ! GetWatchlist(probe.ref)
      probe.expectMessage(WatchlistWrapper(Seq()))
    }

    //ADD
    "add entries to the watchlist" in {
      val opId = "operation1"
      val opId2 = "operation2"
      val ref = spawn(OperationDatabaseBehaviour.create(PersistenceId("fake-type-hint", "fake-id-2")))
      val probe1 = createTestProbe[Confirmation]()
      ref ! AddToWatchlist(opId, probe1.ref)
      probe1.expectMessageType[Accepted]

      val probe2 = createTestProbe[Confirmation]()
      ref ! AddToWatchlist(opId2, probe2.ref)
      probe2.expectMessageType[Accepted]

      val probe3 = createTestProbe[WatchlistWrapper]()
      ref ! GetWatchlist(probe3.ref)
      probe3.expectMessage(WatchlistWrapper(Seq(opId, opId2)))
    }

    "try to add duplicate entry and not get duplicates in watchlist" in {
      val opId = "operation1"
      val ref = spawn(OperationDatabaseBehaviour.create(PersistenceId("fake-type-hint", "fake-id-3")))
      val probe1 = createTestProbe[Confirmation]()
      ref ! AddToWatchlist(opId, probe1.ref)
      probe1.expectMessageType[Accepted]

      val probe2 = createTestProbe[Confirmation]()
      ref ! AddToWatchlist(opId, probe2.ref)
      probe2.expectMessageType[Accepted]

      val probe3 = createTestProbe[WatchlistWrapper]()
      ref ! GetWatchlist(probe3.ref)
      probe3.expectMessage(WatchlistWrapper(Seq(opId)))
    }

    //DELETE
    "not fail on trying to delete a non-existing operationId" in {
      val opId = "operation1"
      val ref = spawn(OperationDatabaseBehaviour.create(PersistenceId("fake-type-hint", "fake-id-4")))
      val probe1 = createTestProbe[Confirmation]()
      ref ! AddToWatchlist(opId, probe1.ref)
      probe1.expectMessageType[Accepted]

      val probe2 = createTestProbe[WatchlistWrapper]()
      ref ! GetWatchlist(probe2.ref)
      probe2.expectMessage(WatchlistWrapper(Seq(opId)))

      val probe3 = createTestProbe[Confirmation]()
      ref ! RemoveFromWatchlist("not" + opId, probe3.ref)
      probe3.expectMessageType[Accepted]

      val probe4 = createTestProbe[WatchlistWrapper]()
      ref ! GetWatchlist(probe4.ref)
      probe4.expectMessage(WatchlistWrapper(Seq(opId)))
    }

    "delete an entry from the watchlist" in {
      val opId = "operation1"
      val ref = spawn(OperationDatabaseBehaviour.create(PersistenceId("fake-type-hint", "fake-id-5")))
      val probe1 = createTestProbe[Confirmation]()
      ref ! AddToWatchlist(opId, probe1.ref)
      probe1.expectMessageType[Accepted]

      val probe2 = createTestProbe[WatchlistWrapper]()
      ref ! GetWatchlist(probe2.ref)
      probe2.expectMessage(WatchlistWrapper(Seq(opId)))

      val probe3 = createTestProbe[Confirmation]()
      ref ! RemoveFromWatchlist(opId, probe3.ref)
      probe3.expectMessageType[Accepted]

      val probe4 = createTestProbe[WatchlistWrapper]()
      ref ! GetWatchlist(probe4.ref)
      probe4.expectMessage(WatchlistWrapper(Seq()))
    }

    "clear the watchlist" in {
      val opId = "operation1"
      val ref = spawn(OperationDatabaseBehaviour.create(PersistenceId("fake-type-hint", "fake-id-6")))
      val probe1 = createTestProbe[Confirmation]()
      ref ! AddToWatchlist(opId, probe1.ref)
      probe1.expectMessageType[Accepted]

      val probe2 = createTestProbe[WatchlistWrapper]()
      ref ! GetWatchlist(probe2.ref)
      probe2.expectMessage(WatchlistWrapper(Seq(opId)))

      val probe3 = createTestProbe[Confirmation]()
      ref ! ClearWatchlist("username", probe3.ref)
      probe3.expectMessageType[Accepted]

      val probe4 = createTestProbe[WatchlistWrapper]()
      ref ! GetWatchlist(probe4.ref)
      probe4.expectMessage(WatchlistWrapper(Seq()))
    }
  }
}
