package de.upb.cs.uc4.certificate.impl

import java.util.UUID

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.persistence.typed.PersistenceId
import de.upb.cs.uc4.certificate.impl.actor.CertificateBehaviour
import de.upb.cs.uc4.certificate.impl.commands.{ DeleteCertificateUser, GetCertificateUser, RegisterUser, SetCertificateAndKey }
import de.upb.cs.uc4.certificate.model.EncryptedPrivateKey
import de.upb.cs.uc4.shared.server.messages.{ Accepted, Confirmation }
import de.upb.cs.uc4.user.model.Role
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

/** Tests for the CertificateState */
class CertificateStateSpec extends ScalaTestWithActorTestKit(s"""
      akka.persistence.journal.plugin = "akka.persistence.journal.inmem"
      akka.persistence.snapshot-store.plugin = "akka.persistence.snapshot-store.local"
      akka.persistence.snapshot-store.local.dir = "target/snapshot-${UUID.randomUUID().toString}"
    """) with AnyWordSpecLike with Matchers {

  "CertificateState" should {

    //FETCH
    "get a CertificateUser" in {
      val probe = createTestProbe[(Option[String], Option[String], Option[String], Option[EncryptedPrivateKey])]()
      val ref = spawn(CertificateBehaviour.create(PersistenceId("fake-type-hint", "fake-id-1")))
      ref ! GetCertificateUser(probe.ref)
      probe.expectMessage((None, None, None, None))
    }

    "get a registered CertificateUser" in {
      val ref = spawn(CertificateBehaviour.create(PersistenceId("fake-type-hint", "fake-id-2")))

      //register a user
      val probe1 = createTestProbe[Confirmation]()
      ref ! RegisterUser("enrollmentId", "enrollmentSecret", probe1.ref)
      probe1.expectMessage(Accepted)

      //fetch a registered user
      val probe2 = createTestProbe[(Option[String], Option[String], Option[String], Option[EncryptedPrivateKey])]()
      ref ! GetCertificateUser(probe2.ref)
      probe2.expectMessage((Some("enrollmentId"), Some("enrollmentSecret"), None, None))
    }

    "get the enrollmentId, enrollmentSecret, certificate and key of a CertificateUser" in {
      val ref = spawn(CertificateBehaviour.create(PersistenceId("fake-type-hint", "fake-id-4")))

      //register a user
      val probe1 = createTestProbe[Confirmation]()
      ref ! RegisterUser("enrollmentId", "enrollmentSecret", probe1.ref)
      probe1.expectMessage(Accepted)

      //set the certificate and key of a user
      val probe2 = createTestProbe[Confirmation]()
      ref ! SetCertificateAndKey("certificate", EncryptedPrivateKey("key", "iv", "salt"), probe2.ref)
      probe2.expectMessage(Accepted)

      //fetch a registered user
      val probe3 = createTestProbe[(Option[String], Option[String], Option[String], Option[EncryptedPrivateKey])]()
      ref ! GetCertificateUser(probe3.ref)
      probe3.expectMessage((Some("enrollmentId"), Some("enrollmentSecret"), Some("certificate"), Some(EncryptedPrivateKey("key", "iv", "salt"))))
    }

    //REGISTER
    "register a CertificateUser" in {
      val probe = createTestProbe[Confirmation]()
      val ref = spawn(CertificateBehaviour.create(PersistenceId("fake-type-hint", "fake-id-5")))
      ref ! RegisterUser("enrollmentId", "enrollmentSecret", probe.ref)
      probe.expectMessage(Accepted)
    }

    //SET CERTIFICATE AND KEY
    "set the certificate and key of a CertificateUser" in {
      val ref = spawn(CertificateBehaviour.create(PersistenceId("fake-type-hint", "fake-id-6")))

      //register a user
      val probe1 = createTestProbe[Confirmation]()
      ref ! RegisterUser("enrollmentId", "enrollmentSecret", probe1.ref)
      probe1.expectMessage(Accepted)

      //set the certificate and key of a user
      val probe2 = createTestProbe[Confirmation]()
      ref ! SetCertificateAndKey("certificate", EncryptedPrivateKey("key", "iv", "salt"), probe2.ref)
      probe2.expectMessage(Accepted)
    }

    //DELETE
    "delete a non-lecturer CertificateUser" in {
      val probe1 = createTestProbe[Confirmation]()
      val probe2 = createTestProbe[Confirmation]()
      val probe3 = createTestProbe[(Option[String], Option[String], Option[String], Option[EncryptedPrivateKey])]()

      val ref = spawn(CertificateBehaviour.create(PersistenceId("fake-type-hint", "fake-id-7")))

      ref ! RegisterUser("enrollmentId", "enrollmentSecret", probe1.ref)
      probe1.expectMessage(Accepted)

      ref ! DeleteCertificateUser("SomeUsername", Role.Student, probe2.ref)
      probe2.expectMessage(Accepted)

      ref ! GetCertificateUser(probe3.ref)
      probe3.expectMessage((None, None, None, None))
    }

    "delete a lecturer CertificateUser" in {
      val probe1 = createTestProbe[Confirmation]()
      val probe2 = createTestProbe[Confirmation]()
      val probe3 = createTestProbe[Confirmation]()
      val probe4 = createTestProbe[(Option[String], Option[String], Option[String], Option[EncryptedPrivateKey])]()

      val ref = spawn(CertificateBehaviour.create(PersistenceId("fake-type-hint", "fake-id-8")))

      ref ! RegisterUser("enrollmentId", "enrollmentSecret", probe1.ref)
      probe1.expectMessage(Accepted)

      ref ! SetCertificateAndKey("certificate", EncryptedPrivateKey("Key", "IV", "Salt"), probe2.ref)
      probe2.expectMessage(Accepted)

      ref ! DeleteCertificateUser("SomeUsername", Role.Lecturer, probe3.ref)
      probe3.expectMessage(Accepted)

      ref ! GetCertificateUser(probe4.ref)
      probe4.expectMessage((Some("enrollmentId"), Some("enrollmentSecret"), Some("certificate"), Some(EncryptedPrivateKey("", "", ""))))
    }
  }
}
