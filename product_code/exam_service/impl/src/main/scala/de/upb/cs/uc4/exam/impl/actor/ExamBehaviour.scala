package de.upb.cs.uc4.exam.impl.actor

import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import akka.pattern.StatusReply
import com.typesafe.config.Config
import de.upb.cs.uc4.exam.impl.commands.{ GetExams, GetProposalAddExam }
import de.upb.cs.uc4.exam.model.Exam
import de.upb.cs.uc4.hyperledger.connections.cases.ConnectionExam
import de.upb.cs.uc4.hyperledger.connections.traits.ConnectionExamTrait
import de.upb.cs.uc4.hyperledger.impl.commands.{ HyperledgerBaseCommand, HyperledgerCommand }
import de.upb.cs.uc4.hyperledger.impl.{ HyperledgerActor, HyperledgerActorObject, ProposalWrapper }
import de.upb.cs.uc4.shared.client.JsonUtility.{ FromJsonUtil, ToJsonUtil }

class ExamBehaviour(val config: Config) extends HyperledgerActor[ConnectionExamTrait] {

  /** Creates the connection to the chaincode */
  override protected def createConnection: ConnectionExamTrait =
    ConnectionExam(adminUsername, channel, chaincode, walletPath, networkDescriptionPath)

  override protected def applyCommand(connection: ConnectionExamTrait, command: HyperledgerCommand[_]): Unit = command match {
    case GetExams(examIds, courseIds, lecturerIds, moduleIds, types, admittableAt, droppableAt, replyTo) =>
      replyTo ! StatusReply.success(ExamsWrapper(
        connection.getExams(examIds.toList, courseIds.toList, lecturerIds.toList, moduleIds.toList, types.toList, admittableAt.getOrElse(""), droppableAt.getOrElse("")).fromJson[Seq[Exam]]
      ))

    case GetProposalAddExam(certificate, exam, replyTo) =>
      replyTo ! StatusReply.success(ProposalWrapper(
        connection.getProposalAddExam(certificate, examJson = exam.toJson)
      ))
  }

  /** The companion object */
  override val companionObject: HyperledgerActorObject = ExamBehaviour
}

object ExamBehaviour extends HyperledgerActorObject {
  /** The EntityTypeKey of this actor */
  override val typeKey: EntityTypeKey[HyperledgerBaseCommand] = EntityTypeKey[HyperledgerBaseCommand]("uc4exam")
}
