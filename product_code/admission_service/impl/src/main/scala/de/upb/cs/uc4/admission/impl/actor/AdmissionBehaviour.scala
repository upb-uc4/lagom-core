package de.upb.cs.uc4.admission.impl.actor

import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import akka.pattern.StatusReply
import com.typesafe.config.Config
import de.upb.cs.uc4.admission.impl.commands.{ GetCourseAdmissions, GetProposalForAddCourseAdmission, GetProposalForDropCourseAdmission }
import de.upb.cs.uc4.admission.model.CourseAdmission
import de.upb.cs.uc4.hyperledger.connections.cases.ConnectionAdmission
import de.upb.cs.uc4.hyperledger.connections.traits.ConnectionAdmissionTrait
import de.upb.cs.uc4.hyperledger.impl.commands.{ HyperledgerBaseCommand, HyperledgerCommand, HyperledgerReadCommand, HyperledgerWriteCommand }
import de.upb.cs.uc4.hyperledger.impl.{ HyperledgerActor, HyperledgerActorObject, ProposalWrapper }
import de.upb.cs.uc4.shared.client.JsonUtility._

class AdmissionBehaviour(val config: Config) extends HyperledgerActor[ConnectionAdmissionTrait] {

  /** Creates the connection to the chaincode */
  override protected def createConnection: ConnectionAdmissionTrait =
    ConnectionAdmission(adminUsername, channel, chaincode, walletPath, networkDescriptionPath)

  /** Gets called every time when the actor receives a command
    * Errors which this method will thrown will be handled accordingly
    * if the command implements [[HyperledgerReadCommand]] or the
    * [[HyperledgerWriteCommand]].
    *
    * @param connection the current active connection
    * @param command which should get executed
    */
  override protected def applyCommand(connection: ConnectionAdmissionTrait, command: HyperledgerCommand[_]): Unit = command match {
    case GetCourseAdmissions(enrollmentId, courseId, moduleId, replyTo) =>
      replyTo ! StatusReply.success(AdmissionsWrapper(
        connection.getAdmissions(enrollmentId.getOrElse(""), courseId.getOrElse(""), moduleId.getOrElse("")).fromJson[Seq[CourseAdmission]]
      ))
    case GetProposalForAddCourseAdmission(certificate, courseAdmission, replyTo) =>
      replyTo ! StatusReply.success(ProposalWrapper(connection.getProposalAddAdmission(certificate, admission = courseAdmission.toJson)))
    case GetProposalForDropCourseAdmission(certificate, dropAdmission, replyTo) =>
      replyTo ! StatusReply.success(ProposalWrapper(connection.getProposalDropAdmission(certificate, admissionId = dropAdmission.admissionId)))
    case _ => println("Unknown command")
  }

  /** The companion object */
  override val companionObject: HyperledgerActorObject = AdmissionBehaviour
}

object AdmissionBehaviour extends HyperledgerActorObject {
  /** The EntityTypeKey of this actor */
  override val typeKey: EntityTypeKey[HyperledgerBaseCommand] = EntityTypeKey[HyperledgerBaseCommand]("uc4admission")
}

