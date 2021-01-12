package de.upb.cs.uc4.operation.impl

import com.lightbend.lagom.scaladsl.persistence.jdbc.JdbcPersistenceComponents
import com.lightbend.lagom.scaladsl.persistence.slick.SlickPersistenceComponents
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.server.{ LagomApplicationContext, LagomServer }
import com.softwaremill.macwire.wire
import de.upb.cs.uc4.certificate.api.CertificateService
import de.upb.cs.uc4.hyperledger.HyperledgerComponent
import de.upb.cs.uc4.matriculation.api.MatriculationService
import de.upb.cs.uc4.operation.api.OperationService
import de.upb.cs.uc4.operation.impl.actor.OperationHyperledgerBehaviour
import de.upb.cs.uc4.operation.impl.readside.{ OperationDatabase, OperationEventProcessor }
import de.upb.cs.uc4.shared.server.UC4Application
import play.api.db.HikariCPComponents

abstract class OperationApplication(context: LagomApplicationContext)
  extends UC4Application(context)
  with SlickPersistenceComponents
  with JdbcPersistenceComponents
  with HikariCPComponents
  with HyperledgerComponent {

  override def createActorFactory: OperationHyperledgerBehaviour = wire[OperationHyperledgerBehaviour]

  // Create ReadSide
  lazy val database: OperationDatabase = wire[OperationDatabase]
  lazy val processor: OperationEventProcessor = wire[OperationEventProcessor]
  readSide.register(processor)

  // Bind UserService
  lazy val matriculationService: MatriculationService = serviceClient.implement[MatriculationService]

  //Bind CertificateService
  lazy val certificateService: CertificateService = serviceClient.implement[CertificateService]

  // Register the JSON serializer registry
  override lazy val jsonSerializerRegistry: JsonSerializerRegistry = OperationSerializerRegistry

  // Bind the service that this server provides
  override lazy val lagomServer: LagomServer = serverFor[OperationService](wire[OperationServiceImpl])
}

object OperationApplication {
  /** Functions as offset for the database */
  val offset: String = "UniversityCredits4Operation"
}
