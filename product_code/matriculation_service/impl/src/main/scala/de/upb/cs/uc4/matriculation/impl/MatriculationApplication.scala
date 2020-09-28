package de.upb.cs.uc4.matriculation.impl

import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.server.{ LagomApplicationContext, LagomServer }
import com.softwaremill.macwire.wire
import de.upb.cs.uc4.hyperledger.HyperledgerComponent
import de.upb.cs.uc4.matriculation.api.MatriculationService
import de.upb.cs.uc4.matriculation.impl.actor.MatriculationBehaviour
import de.upb.cs.uc4.shared.server.UC4Application
import de.upb.cs.uc4.user.api.UserService

abstract class MatriculationApplication(context: LagomApplicationContext)
  extends UC4Application(context)
  with HyperledgerComponent {

  override def createActorFactory: MatriculationBehaviour = wire[MatriculationBehaviour]

  // Bind UserService
  lazy val userService: UserService = serviceClient.implement[UserService]

  // Register the JSON serializer registry
  override lazy val jsonSerializerRegistry: JsonSerializerRegistry = MatriculationSerializerRegistry

  // Bind the service that this server provides
  override lazy val lagomServer: LagomServer = serverFor[MatriculationService](wire[MatriculationServiceImpl])
}
