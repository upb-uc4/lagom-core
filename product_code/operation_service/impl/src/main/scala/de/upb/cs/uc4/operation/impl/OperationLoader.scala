package de.upb.cs.uc4.operation.impl

import com.lightbend.lagom.scaladsl.akka.discovery.AkkaDiscoveryComponents
import com.lightbend.lagom.scaladsl.api.Descriptor
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaClientComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.server._
import de.upb.cs.uc4.operation.api.OperationService

class OperationLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new OperationApplication(context) with AkkaDiscoveryComponents with LagomKafkaClientComponents

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new OperationApplication(context) with LagomDevModeComponents with LagomKafkaClientComponents

  override def describeService: Some[Descriptor] = Some(readDescriptor[OperationService])
}

