package de.upb.cs.uc4.matriculation.impl

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import de.upb.cs.uc4.authentication.api.AuthenticationService
import de.upb.cs.uc4.matriculation.api.MatriculationService
import de.upb.cs.uc4.matriculation.model.ImmatriculationData
import de.upb.cs.uc4.shared.server.ServiceCallFactory._
import de.upb.cs.uc4.shared.server.hyperledger.HyperLedgerSession

import scala.concurrent.ExecutionContext

/** Implementation of the MatriculationService */
class MatriculationServiceImpl(hyperLedgerSession: HyperLedgerSession)
                              (implicit ec: ExecutionContext, auth: AuthenticationService) extends MatriculationService {

  /** Immatriculates a student */
  override def immatriculateStudent(): ServiceCall[ImmatriculationData, Done] = ServiceCall { data =>
    hyperLedgerSession.write[ImmatriculationData]("addStudent", data)
  }

  /** Returns the ImmatriculationData of a student with the given matriculationId */
  override def getMatriculation(matriculationId: String): ServiceCall[NotUsed, ImmatriculationData] = ServiceCall { _ =>
    hyperLedgerSession.read[ImmatriculationData]("getStudent", matriculationId)
  }

  /** Allows GET */
  override def allowedGet: ServiceCall[NotUsed, Done] = allowedMethodsCustom("GET")
}
