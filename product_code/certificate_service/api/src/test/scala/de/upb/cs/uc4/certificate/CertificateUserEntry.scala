package de.upb.cs.uc4.certificate

import de.upb.cs.uc4.certificate.model.EncryptedPrivateKey

/** Helper class for CertificateServiceStub, used to represent the data stored in a single actor
  *
  */
case class CertificateUserEntry(enrollmentId: String, enrollmentSecret: String, certificate: String, encryptedPrivateKey: EncryptedPrivateKey)
