package de.upb.cs.uc4.certificate.impl.events

import de.upb.cs.uc4.certificate.model.EncryptedPrivateKey

case class OnCertficateAndKeySet(certificate: String, encryptedPrivateKey: EncryptedPrivateKey) extends CertificateEvent


