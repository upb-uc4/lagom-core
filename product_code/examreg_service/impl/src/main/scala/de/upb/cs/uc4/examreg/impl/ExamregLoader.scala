package de.upb.cs.uc4.examreg.impl

import com.lightbend.lagom.scaladsl.akka.discovery.AkkaDiscoveryComponents
import com.lightbend.lagom.scaladsl.api.Descriptor
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.server._
import de.upb.cs.uc4.examreg.api.ExamregService

class ExamregLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new ExamregApplication(context) with AkkaDiscoveryComponents

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new ExamregApplication(context) with LagomDevModeComponents

  override def describeService: Some[Descriptor] = Some(readDescriptor[ExamregService])
}

