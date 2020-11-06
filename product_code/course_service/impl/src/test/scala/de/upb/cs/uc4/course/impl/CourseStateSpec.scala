package de.upb.cs.uc4.course.impl

import java.util.UUID

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.persistence.typed.PersistenceId
import de.upb.cs.uc4.course.impl.actor.CourseBehaviour
import de.upb.cs.uc4.course.impl.commands.{ CreateCourse, DeleteCourse, GetCourse, UpdateCourse }
import de.upb.cs.uc4.course.model.Course
import de.upb.cs.uc4.shared.client.configuration.{ CourseLanguage, CourseType }
import de.upb.cs.uc4.shared.server.messages.{ Accepted, Confirmation, Rejected }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

/** Tests for the CourseState */
class CourseStateSpec extends ScalaTestWithActorTestKit(s"""
      akka.persistence.journal.plugin = "akka.persistence.journal.inmem"
      akka.persistence.snapshot-store.plugin = "akka.persistence.snapshot-store.local"
      akka.persistence.snapshot-store.local.dir = "target/snapshot-${UUID.randomUUID().toString}"
    """) with AnyWordSpecLike with Matchers {

  //Test courses
  val course0: Course = Course("18", Seq(), "Course 0", CourseType.Lecture.toString, "2020-04-11", "2020-08-01", 8, "11", 60, 20, CourseLanguage.German.toString, "A test")
  val course1: Course = Course("17", Seq(), "Course 1", CourseType.Lecture.toString, "2020-04-11", "2020-08-01", 8, "11", 60, 20, CourseLanguage.German.toString, "A test")
  val course2: Course = Course("16", Seq(), "Course 1", CourseType.Lecture.toString, "2020-04-11", "2020-08-01", 8, "12", 60, 20, CourseLanguage.German.toString, "A test")
  val course3: Course = Course("18", Seq(), "Course 3", CourseType.Lecture.toString, "2020-04-11", "2020-08-01", 8, "11", 60, 20, CourseLanguage.German.toString, "A test")

  "CourseState" should {

    "get non-existing course" in {
      val probe = createTestProbe[Option[Course]]()
      val ref = spawn(CourseBehaviour.create(PersistenceId("fake-type-hint", "fake-id-1")))
      ref ! GetCourse(probe.ref)
      probe.expectMessage(None)
    }

    "create a course" in {
      val ref = spawn(CourseBehaviour.create(PersistenceId("fake-type-hint", "fake-id-2")))

      val probe1 = createTestProbe[Confirmation]()
      ref ! CreateCourse(course0, probe1.ref)
      probe1.expectMessage(Accepted)

      val probe2 = createTestProbe[Option[Course]]()
      ref ! GetCourse(probe2.ref)
      probe2.expectMessage(Some(course0))
    }

    "create an already existing course" in {
      val ref = spawn(CourseBehaviour.create(PersistenceId("fake-type-hint", "fake-id-3")))

      val probe1 = createTestProbe[Confirmation]()
      ref ! CreateCourse(course0, probe1.ref)
      probe1.expectMessage(Accepted)

      val probe2 = createTestProbe[Confirmation]()
      ref ! CreateCourse(course3, probe2.ref)
      probe2.expectMessageType[Rejected]
    }

    "update a non-existing course" in {
      val ref = spawn(CourseBehaviour.create(PersistenceId("fake-type-hint", "fake-id-4")))

      val probe1 = createTestProbe[Confirmation]()
      ref ! UpdateCourse(course3, probe1.ref)
      probe1.expectMessageType[Rejected]
    }

    "update an existing course" in {
      val ref = spawn(CourseBehaviour.create(PersistenceId("fake-type-hint", "fake-id-5")))

      val probe1 = createTestProbe[Confirmation]()
      ref ! CreateCourse(course0, probe1.ref)
      probe1.expectMessage(Accepted)

      val probe2 = createTestProbe[Confirmation]()
      ref ! UpdateCourse(course3, probe2.ref)
      probe2.expectMessage(Accepted)

      val probe3 = createTestProbe[Option[Course]]()
      ref ! GetCourse(probe3.ref)
      probe3.expectMessage(Some(course3))
    }

    "delete a non-existing course" in {
      val ref = spawn(CourseBehaviour.create(PersistenceId("fake-type-hint", "fake-id-6")))

      val probe1 = createTestProbe[Confirmation]()
      ref ! DeleteCourse("69", probe1.ref)
      probe1.expectMessageType[Rejected]
    }

    "delete an existing course" in {
      val ref = spawn(CourseBehaviour.create(PersistenceId("fake-type-hint", "fake-id-7")))

      val probe1 = createTestProbe[Confirmation]()
      ref ! CreateCourse(course0, probe1.ref)
      probe1.expectMessage(Accepted)

      val probe2 = createTestProbe[Confirmation]()
      ref ! DeleteCourse(course0.courseId, probe2.ref)
      probe2.expectMessage(Accepted)

      val probe3 = createTestProbe[Option[Course]]()
      ref ! GetCourse(probe3.ref)
      probe3.expectMessage(None)
    }
  }
}
