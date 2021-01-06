package de.upb.cs.uc4.admission.impl

import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.server.{ LagomApplicationContext, LagomServer }
import com.softwaremill.macwire.wire
import de.upb.cs.uc4.admission.api.AdmissionService
import de.upb.cs.uc4.admission.impl.actor.AdmissionBehaviour
import de.upb.cs.uc4.certificate.api.CertificateService
import de.upb.cs.uc4.course.api.CourseService
import de.upb.cs.uc4.examreg.api.ExamregService
import de.upb.cs.uc4.hyperledger.HyperledgerComponent
import de.upb.cs.uc4.matriculation.api.MatriculationService
import de.upb.cs.uc4.shared.server.UC4Application

abstract class AdmissionApplication(context: LagomApplicationContext)
  extends UC4Application(context)
  with HyperledgerComponent {

  override def createActorFactory: AdmissionBehaviour = wire[AdmissionBehaviour]

  // Bind Services
  lazy val matriculationService: MatriculationService = serviceClient.implement[MatriculationService]
  lazy val examRegService: ExamregService = serviceClient.implement[ExamregService]
  lazy val courseService: CourseService = serviceClient.implement[CourseService]
  lazy val certificateService: CertificateService = serviceClient.implement[CertificateService]
  lazy val admissionService: AdmissionService = serviceClient.implement[AdmissionService]

  // Register the JSON serializer registry
  override lazy val jsonSerializerRegistry: JsonSerializerRegistry = AdmissionSerializerRegistry

  // Bind the service that this server provides
  override lazy val lagomServer: LagomServer = serverFor[AdmissionService](wire[AdmissionServiceImpl])
}
