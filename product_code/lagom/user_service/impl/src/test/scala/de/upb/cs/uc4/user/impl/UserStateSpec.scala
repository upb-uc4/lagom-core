package de.upb.cs.uc4.user.impl

import java.util.UUID

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.persistence.typed.PersistenceId
import de.upb.cs.uc4.authentication.model.{AuthenticationRole, AuthenticationUser}
import de.upb.cs.uc4.shared.server.messages.{Accepted, Confirmation, Rejected, RejectedWithError}
import de.upb.cs.uc4.user.impl.actor.UserBehaviour
import de.upb.cs.uc4.user.impl.commands.{CreateUser, DeleteUser, GetUser, UpdateUser}
import de.upb.cs.uc4.user.model.user._
import de.upb.cs.uc4.user.model.{Address, Role}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

/** Tests for the CourseState */
class UserStateSpec extends ScalaTestWithActorTestKit(s"""
      akka.persistence.journal.plugin = "akka.persistence.journal.inmem"
      akka.persistence.snapshot-store.plugin = "akka.persistence.snapshot-store.local"
      akka.persistence.snapshot-store.local.dir = "target/snapshot-${UUID.randomUUID().toString}"
    """) with AnyWordSpecLike with Matchers {

  //Test users
  val address: Address = Address("ExampleStreet", "42a", "13337", "ExampleCity", "ExampleCountry")
  val authenticationUser: AuthenticationUser = AuthenticationUser("MOCK", "MOCK", AuthenticationRole.Admin)

  val student0: Student = Student("student0", Role.Student, address, "firstName", "LastName", "Picture", "example@mail.de", "1990-12-11", "IN", "421769", 9000, List())
  val lecturer0: Lecturer = Lecturer("lecturer0", Role.Lecturer, address, "firstName", "LastName", "Picture", "example@mail.de", "1991-12-11", "Heute kommt der kleine Gauss dran.", "Mathematics")
  val admin0: Admin = Admin("admin0", Role.Admin, address, "firstName", "LastName", "Picture", "example1@mail.de", "1992-12-11")
  val admin1: Admin = Admin("admin0", Role.Admin, address, "firstNameDifferent", "LastNameDifferent", "Picture", "example2@mail.de", "1992-12-11")

  val emptyLecturer: Lecturer = Lecturer("lecturer0",Role.Lecturer,address,"","","","","","","") //name for update test
  
  "UserState" should {

    //GET
    "get non-existing user" in {
      val probe = createTestProbe[Option[User]]()
      val ref = spawn(UserBehaviour.create(PersistenceId("fake-type-hint", "fake-id-1")))
      ref ! GetUser(probe.ref)
      probe.expectMessage(None)
    }

    //ADD
    "add a user" in  {
      val ref = spawn(UserBehaviour.create(PersistenceId("fake-type-hint", "fake-id-2")))

      val probe1 = createTestProbe[Confirmation]()
      ref ! CreateUser(student0, authenticationUser, probe1.ref)
      probe1.expectMessage(Accepted)

      val probe2 = createTestProbe[Option[User]]()
      ref ! GetUser(probe2.ref)
      probe2.expectMessage(Some(student0))
    }

    "not add an already existing user" in {
      val ref = spawn(UserBehaviour.create(PersistenceId("fake-type-hint", "fake-id-3")))

      val probe1 = createTestProbe[Confirmation]()
      ref ! CreateUser(admin0, authenticationUser, probe1.ref)
      probe1.expectMessage(Accepted)

      val probe2 = createTestProbe[Confirmation]()
      ref ! CreateUser(admin1, authenticationUser, probe2.ref)
      val message = probe2.receiveMessage()
      assert(message.isInstanceOf[RejectedWithError] && message.asInstanceOf[RejectedWithError].statusCode == 409)
    }

    "not add a User that does not validate" in {
      val ref = spawn(UserBehaviour.create(PersistenceId("fake-type-hint", "fake-id-8")))

      val probe1 = createTestProbe[Confirmation]()
      ref ! CreateUser(emptyLecturer, authenticationUser, probe1.ref)
      val message = probe1.receiveMessage()
      assert(message.isInstanceOf[RejectedWithError] && message.asInstanceOf[RejectedWithError].statusCode == 422)
    }

    //UPDATE
    "not update a non-existing user" in {
      val ref = spawn(UserBehaviour.create(PersistenceId("fake-type-hint", "fake-id-4")))

      val probe1 = createTestProbe[Confirmation]()
      ref ! UpdateUser(lecturer0, probe1.ref)
      val message = probe1.receiveMessage()
      assert(message.isInstanceOf[RejectedWithError] && message.asInstanceOf[RejectedWithError].statusCode == 404)
    }

    "not update a user with invalid data" in {
      val ref = spawn(UserBehaviour.create(PersistenceId("fake-type-hint", "fake-id-4.1")))

      val probe1 = createTestProbe[Confirmation]()
      ref ! CreateUser(lecturer0, authenticationUser, probe1.ref)
      probe1.expectMessage(Accepted)

      val probe2 = createTestProbe[Confirmation]()
      ref ! UpdateUser(emptyLecturer, probe2.ref)
      val message = probe2.receiveMessage()
      assert(message.isInstanceOf[RejectedWithError] && message.asInstanceOf[RejectedWithError].statusCode == 422)
    }

    "update an existing user" in {
      val ref = spawn(UserBehaviour.create(PersistenceId("fake-type-hint", "fake-id-5")))

      val probe1 = createTestProbe[Confirmation]()
      ref ! CreateUser(admin0, authenticationUser, probe1.ref)
      probe1.expectMessage(Accepted)

      val probe2 = createTestProbe[Confirmation]()
      ref ! UpdateUser(admin1, probe2.ref)
      probe2.expectMessage(Accepted)

      val probe3 = createTestProbe[Option[User]]()
      ref ! GetUser(probe3.ref)
      probe3.expectMessage(Some(admin1))
    }

    //DELETE
    "not delete a non-existing user" in {
      val ref = spawn(UserBehaviour.create(PersistenceId("fake-type-hint", "fake-id-6")))

      val probe1 = createTestProbe[Confirmation]()
      ref ! DeleteUser(probe1.ref)
      probe1.expectMessageType[Rejected]
    }

    "delete an existing user" in {
      val ref = spawn(UserBehaviour.create(PersistenceId("fake-type-hint", "fake-id-7")))

      val probe1 = createTestProbe[Confirmation]()
      ref ! CreateUser(lecturer0, authenticationUser, probe1.ref)
      probe1.expectMessage(Accepted)

      val probe2 = createTestProbe[Confirmation]()
      ref ! DeleteUser(probe2.ref)
      probe2.expectMessage(Accepted)

      val probe3 = createTestProbe[Option[User]]()
      ref ! GetUser(probe3.ref)
      probe3.expectMessage(None)
    }
  }
}
