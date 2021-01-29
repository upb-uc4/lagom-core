package de.upb.cs.uc4.certificate.impl.readside

import com.lightbend.lagom.scaladsl.persistence.slick.SlickReadSide
import com.lightbend.lagom.scaladsl.persistence.{ AggregateEventTag, ReadSideProcessor }
import de.upb.cs.uc4.certificate.impl.CertificateApplication
import de.upb.cs.uc4.certificate.impl.events.{ CertificateEvent, OnCertificateUserForceDelete, OnCertificateUserSoftDelete, OnRegisterUser }
import de.upb.cs.uc4.user.model.Role
import slick.dbio.DBIOAction

class CertificateEventProcessor(readSide: SlickReadSide, database: CertificateDatabase)
  extends ReadSideProcessor[CertificateEvent] {

  /** @inheritdoc */
  override def buildHandler(): ReadSideProcessor.ReadSideHandler[CertificateEvent] =
    readSide.builder[CertificateEvent](CertificateApplication.offset)
      .setGlobalPrepare(database.createTable())
      .setEventHandler[OnRegisterUser] { envelope =>
        database.setEnrollmentId(envelope.event.username, envelope.event.enrollmentId)
      }
      .setEventHandler[OnCertificateUserForceDelete] { envelope =>
        database.deleteEnrollmentId(envelope.event.username)
      }
      .setEventHandler[OnCertificateUserSoftDelete] { envelope =>
        if (envelope.event.role != Role.Lecturer) {
          database.deleteEnrollmentId(envelope.event.username)
        }
        else {
          DBIOAction.successful(Nil)
        }
      }
      .build()

  /** @inheritdoc */
  override def aggregateTags: Set[AggregateEventTag[CertificateEvent]] = Set(CertificateEvent.Tag)
}
