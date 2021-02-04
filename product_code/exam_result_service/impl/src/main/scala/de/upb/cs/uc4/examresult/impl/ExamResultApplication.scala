package de.upb.cs.uc4.examresult.impl

import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.server.{ LagomApplicationContext, LagomServer }
import com.softwaremill.macwire.wire
import de.upb.cs.uc4.exam.api.ExamService
import de.upb.cs.uc4.examresult.api.ExamResultService
import de.upb.cs.uc4.examresult.impl.actor.ExamResultBehaviour
import de.upb.cs.uc4.hyperledger.HyperledgerComponent
import de.upb.cs.uc4.operation.api.OperationService
import de.upb.cs.uc4.shared.server.UC4Application

abstract class ExamResultApplication(context: LagomApplicationContext)
  extends UC4Application(context)
  with HyperledgerComponent {

  override def createActorFactory: ExamResultBehaviour = wire[ExamResultBehaviour]

  lazy val examService: ExamService = serviceClient.implement[ExamService]
  lazy val operationService: OperationService = serviceClient.implement[OperationService]

  // Register the JSON serializer registry
  override lazy val jsonSerializerRegistry: JsonSerializerRegistry = ExamResultSerializerRegistry

  // Bind the service that this server provides
  override lazy val lagomServer: LagomServer = serverFor[ExamResultService](wire[ExamResultServiceImpl])
}
