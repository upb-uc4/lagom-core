package de.upb.cs.uc4.impl

import java.util.UUID

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike


class CourseStateSpec extends ScalaTestWithActorTestKit(s"""
      akka.persistence.journal.plugin = "akka.persistence.journal.inmem"
      akka.persistence.snapshot-store.plugin = "akka.persistence.snapshot-store.local"
      akka.persistence.snapshot-store.local.dir = "target/snapshot-${UUID.randomUUID().toString}"
    """) with AnyWordSpecLike with Matchers {


  "CourseState" should {

    "get default memory" in {
      val probe = createTestProbe[Content]()
      val ref = spawn(MemoryBehaviour.create(PersistenceId("fake-type-hint", "fake-id")))
      ref ! Get(probe.ref)
      probe.expectMessage(Content(List()))
    }

    "allow extending the memory" in  {
      val ref = spawn(MemoryBehaviour.create(PersistenceId("fake-type-hint", "fake-id")))

      val probe1 = createTestProbe[Confirmation]()
      ref ! Store("fake-id", "First", probe1.ref)
      probe1.expectMessage(Accepted)

      val probe2 = createTestProbe[Content]()
      ref ! Get(probe2.ref)
      probe2.expectMessage(Content(List("First")))
    }

  }
}
