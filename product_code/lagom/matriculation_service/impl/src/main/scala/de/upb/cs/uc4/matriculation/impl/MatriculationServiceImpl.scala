package de.upb.cs.uc4.matriculation.impl

import akka.Done
import com.lightbend.lagom.scaladsl.api.ServiceCall
import de.upb.cs.uc4.authentication.api.AuthenticationService
import de.upb.cs.uc4.matriculation.api.MatriculationService
import de.upb.cs.uc4.matriculation.model.ImmatriculationData
import de.upb.cs.uc4.shared.server.hyperledger.HyperLedgerSession

import scala.concurrent.ExecutionContext

/** Implementation of the MatriculationService */
class MatriculationServiceImpl(hyperLedgerSession: HyperLedgerSession)
                              (implicit ec: ExecutionContext, auth: AuthenticationService) extends MatriculationService {

  override def immatriculateStudent(): ServiceCall[ImmatriculationData, Done] = ???
}
