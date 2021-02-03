package de.upb.cs.uc4.exam.impl

import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.server.{ LagomApplicationContext, LagomServer }
import com.softwaremill.macwire.wire
import de.upb.cs.uc4.course.api.CourseService
import de.upb.cs.uc4.exam.api.ExamService
import de.upb.cs.uc4.exam.impl.actor.ExamBehaviour
import de.upb.cs.uc4.hyperledger.HyperledgerComponent
import de.upb.cs.uc4.operation.api.OperationService
import de.upb.cs.uc4.shared.server.UC4Application

abstract class ExamApplication(context: LagomApplicationContext)
  extends UC4Application(context)
  with HyperledgerComponent {

  //TODO adapt
  //override def createActorFactory: ExamBehaviour = wire[ExamBehaviour]

  lazy val courseService: CourseService = serviceClient.implement[CourseService]
  lazy val operationService: OperationService = serviceClient.implement[OperationService]

  // Register the JSON serializer registry
  override lazy val jsonSerializerRegistry: JsonSerializerRegistry = ExamSerializerRegistry

  // Bind the service that this server provides
  override lazy val lagomServer: LagomServer = serverFor[ExamService](wire[ExamServiceImpl])
}
