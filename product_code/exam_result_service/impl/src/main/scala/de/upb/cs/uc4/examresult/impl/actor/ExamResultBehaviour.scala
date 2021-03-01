package de.upb.cs.uc4.examresult.impl.actor

import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import akka.pattern.StatusReply
import com.typesafe.config.Config
import de.upb.cs.uc4.examresult.impl.commands.{ GetExamResultEntries, GetProposalAddExamResult }
import de.upb.cs.uc4.examresult.model.ExamResultEntry
import de.upb.cs.uc4.hyperledger.connections.cases.ConnectionExamResult
import de.upb.cs.uc4.hyperledger.connections.traits.ConnectionExamResultTrait
import de.upb.cs.uc4.hyperledger.impl.commands.{ HyperledgerBaseCommand, HyperledgerCommand }
import de.upb.cs.uc4.hyperledger.impl.{ HyperledgerActor, HyperledgerActorObject, ProposalWrapper }
import de.upb.cs.uc4.shared.client.JsonUtility.{ FromJsonUtil, ToJsonUtil }

class ExamResultBehaviour(val config: Config) extends HyperledgerActor[ConnectionExamResultTrait] {

  /** Creates the connection to the chaincode */
  override protected def createConnection: ConnectionExamResultTrait =
    ConnectionExamResult(adminUsername, channel, chaincode, walletPath, networkDescriptionPath)

  override protected def applyCommand(connection: ConnectionExamResultTrait, command: HyperledgerCommand[_]): Unit = command match {
    case GetExamResultEntries(enrollmentId, examIds, replyTo) =>
      replyTo ! StatusReply.success(ExamResultWrapper(
        connection.getExamResultEntries(enrollmentId.getOrElse(""), examIds.getOrElse(Seq())).fromJson[Seq[ExamResultEntry]]
      ))

    case GetProposalAddExamResult(certificate, examResult, replyTo) =>
      replyTo ! StatusReply.success(
        ProposalWrapper(connection.getProposalAddExamResult(certificate, examResultJson = examResult.toJson))
      )
  }

  /** The companion object */
  override val companionObject: HyperledgerActorObject = ExamResultBehaviour
}

object ExamResultBehaviour extends HyperledgerActorObject {
  /** The EntityTypeKey of this actor */
  override val typeKey: EntityTypeKey[HyperledgerBaseCommand] = EntityTypeKey[HyperledgerBaseCommand]("uc4examresult")
}
