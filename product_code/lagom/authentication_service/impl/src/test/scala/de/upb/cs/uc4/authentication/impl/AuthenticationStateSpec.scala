package de.upb.cs.uc4.authentication.impl

import java.util.UUID

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.persistence.typed.PersistenceId
import de.upb.cs.uc4.authentication.impl.actor.{AuthenticationBehaviour, AuthenticationEntry}
import de.upb.cs.uc4.authentication.impl.commands.{DeleteAuthentication, GetAuthentication, SetAuthentication}
import de.upb.cs.uc4.authentication.model.{AuthenticationRole, AuthenticationUser}
import de.upb.cs.uc4.shared.server.messages.{Accepted, Confirmation, Rejected}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

/** Tests for the CourseState */
class AuthenticationStateSpec extends ScalaTestWithActorTestKit(s"""
      akka.persistence.journal.plugin = "akka.persistence.journal.inmem"
      akka.persistence.snapshot-store.plugin = "akka.persistence.snapshot-store.local"
      akka.persistence.snapshot-store.local.dir = "target/snapshot-${UUID.randomUUID().toString}"
    """) with AnyWordSpecLike with Matchers {

  //Test users
  val authenticationUser1: AuthenticationUser = AuthenticationUser("ben123", "Hermann", AuthenticationRole.Admin)
  val authenticationUser2: AuthenticationUser = AuthenticationUser("312neb", "Nnamreh", AuthenticationRole.Student)


  "AuthenticationState" should {

    "get non-existing user" in {
      val probe = createTestProbe[Option[AuthenticationEntry]]()
      val ref = spawn(AuthenticationBehaviour.create(PersistenceId("fake-type-hint", "fake-id-1")))
      ref ! GetAuthentication(probe.ref)
      probe.expectMessage(None)
    }

    "set a user" in  {
      val ref = spawn(AuthenticationBehaviour.create(PersistenceId("fake-type-hint", "fake-id-2")))

      val probe1 = createTestProbe[Confirmation]()
      ref ! SetAuthentication(authenticationUser1, probe1.ref)
      probe1.expectMessage(Accepted)

      val probe2 = createTestProbe[Option[AuthenticationEntry]]()
      ref ! GetAuthentication(probe2.ref)
      probe2.expectMessageType[Some[AuthenticationEntry]]
    }

    "not delete a non-existing user" in {
      val ref = spawn(AuthenticationBehaviour.create(PersistenceId("fake-type-hint", "fake-id-6")))

      val probe1 = createTestProbe[Confirmation]()
      ref ! DeleteAuthentication(probe1.ref)
      probe1.expectMessageType[Rejected]
    }

    "delete an existing user" in {
      val ref = spawn(AuthenticationBehaviour.create(PersistenceId("fake-type-hint", "fake-id-7")))

      val probe1 = createTestProbe[Confirmation]()
      ref ! SetAuthentication(authenticationUser2, probe1.ref)
      probe1.expectMessage(Accepted)

      val probe2 = createTestProbe[Confirmation]()
      ref ! DeleteAuthentication(probe2.ref)
      probe2.expectMessage(Accepted)

      val probe3 = createTestProbe[Option[AuthenticationEntry]]()
      ref ! GetAuthentication(probe3.ref)
      probe3.expectMessage(None)
    }
  }
}
