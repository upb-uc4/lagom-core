package de.upb.cs.uc4.certificate.impl

import com.lightbend.lagom.scaladsl.akka.discovery.AkkaDiscoveryComponents
import com.lightbend.lagom.scaladsl.api.Descriptor
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.server._
import de.upb.cs.uc4.certificate.api.CertificateService

class CertificateLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new CertificateApplication(context) with AkkaDiscoveryComponents with LagomKafkaComponents

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new CertificateApplication(context) with LagomDevModeComponents with LagomKafkaComponents

  override def describeService: Some[Descriptor] = Some(readDescriptor[CertificateService])
}

