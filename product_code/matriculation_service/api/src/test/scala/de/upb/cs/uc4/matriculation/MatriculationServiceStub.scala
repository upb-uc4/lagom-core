package de.upb.cs.uc4.matriculation

import akka.{ Done, NotUsed }
import com.lightbend.lagom.scaladsl.api.ServiceCall
import de.upb.cs.uc4.matriculation.api.MatriculationService
import de.upb.cs.uc4.matriculation.model.{ ImmatriculationData, PutMessageMatriculation, SubjectMatriculation }
import de.upb.cs.uc4.shared.client.exceptions.UC4Exception
import de.upb.cs.uc4.shared.client.{ JsonHyperledgerVersion, SignedProposal, SignedTransaction, UnsignedProposal, UnsignedTransaction }

import scala.collection.mutable
import scala.concurrent.Future

class MatriculationServiceStub extends MatriculationService {

  private val matriculationData = mutable.HashMap[String, ImmatriculationData]()

  /** Helper methods for tests */
  def addImmatriculationData(username: String, immatriculationData: ImmatriculationData): Unit = {
    matriculationData.put(username, immatriculationData)
  }

  /** Creates a SubjectMatriculation with one field of study and one semester */
  def createSingleMatriculation(field: String, semester: String) = Seq(SubjectMatriculation(field, Seq(semester)))

  /** Creates an ImmatriculationData object with the given enrollmentId and one field of study and one semester */
  def createSingleImmatriculationData(enrollmentId: String, field: String, semester: String): ImmatriculationData = {
    ImmatriculationData(enrollmentId, createSingleMatriculation(field, semester))
  }

  /** Delete all matriculation data */
  def reset(): Unit = {
    matriculationData.clear()
  }

  /** Submits a transaction to matriculate a student */
  override def submitMatriculationTransaction(username: String): ServiceCall[SignedTransaction, Done] = {
    _ => Future.successful(Done)
  }

  /** Submits a proposal to matriculate a student */
  override def submitMatriculationProposal(username: String): ServiceCall[SignedProposal, UnsignedTransaction] = { _ =>
    Future.successful(UnsignedTransaction(""))
  }

  /** Get proposal to matriculate a student */
  override def getMatriculationProposal(username: String): ServiceCall[PutMessageMatriculation, UnsignedProposal] = { _ =>
    Future.successful(UnsignedProposal(""))
  }

  /** Returns the ImmatriculationData of a student with the given username */
  override def getMatriculationData(username: String): ServiceCall[NotUsed, ImmatriculationData] = { _ =>
    matriculationData.get(username) match {
      case Some(data) => Future.successful(data)
      case None       => Future.failed(UC4Exception.NotFound)
    }
  }

  /** Allows POST */
  override def allowedPost: ServiceCall[NotUsed, Done] = { _ => Future.successful(Done) }

  /** Allows GET */
  override def allowedGet: ServiceCall[NotUsed, Done] = { _ => Future.successful(Done) }

  /** Immatriculates a student */
  override def addMatriculationData(username: String): ServiceCall[PutMessageMatriculation, Done] = { _ => Future.successful(Done) }

  /** Allows PUT */
  override def allowedPut: ServiceCall[NotUsed, Done] = { _ => Future.successful(Done) }

  /** Get the version of the Hyperledger API and the version of the chaincode the service uses */
  override def getHlfVersions: ServiceCall[NotUsed, JsonHyperledgerVersion] = { _ =>
    Future.successful(JsonHyperledgerVersion("Version API", "Version Chaincode"))
  }

  /** This Methods needs to allow a GET-Method */
  override def allowVersionNumber: ServiceCall[NotUsed, Done] = { _ => Future.successful(Done) }
}
