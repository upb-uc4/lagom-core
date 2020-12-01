package de.upb.cs.uc4.examreg.impl.actor

import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import akka.pattern.StatusReply
import com.typesafe.config.Config
import de.upb.cs.uc4.examreg.impl.ExamregApplication
import de.upb.cs.uc4.examreg.impl.commands.{ CloseExamregHyperledger, CreateExamregHyperledger }
import de.upb.cs.uc4.hyperledger.{ HyperledgerActorObject, HyperledgerDefaultActorFactory }
import de.upb.cs.uc4.hyperledger.commands.{ HyperledgerBaseCommand, HyperledgerCommand }
import de.upb.cs.uc4.hyperledger.connections.cases.{ ConnectionExaminationRegulation, ConnectionMatriculation }
import de.upb.cs.uc4.hyperledger.connections.traits.ConnectionExaminationRegulationTrait
import de.upb.cs.uc4.hyperledger.HyperledgerUtils.JsonUtil._
import de.upb.cs.uc4.shared.server.messages.Accepted

import scala.util.Success

class ExamregHyperledgerBehaviour(val config: Config) extends HyperledgerDefaultActorFactory[ConnectionExaminationRegulationTrait] {

  /** Creates the connection to the chaincode */
  override protected def createConnection: ConnectionExaminationRegulationTrait =
    ConnectionExaminationRegulation(adminUsername, channel, chaincode, walletPath, networkDescriptionPath)

  /** Gets called every time when the actor receives a command
    * Errors which this method will thrown will be handled accordingly
    * if the command implements [[de.upb.cs.uc4.hyperledger.commands.HyperledgerReadCommand]] or the
    * [[de.upb.cs.uc4.hyperledger.commands.HyperledgerWriteCommand]].
    *
    * @param connection the current active connection
    * @param command which should get executed
    */
  override protected def applyCommand(connection: ConnectionExaminationRegulationTrait, command: HyperledgerCommand[_]): Unit = command match {

    case CreateExamregHyperledger(examreg, replyTo) =>
      connection.addExaminationRegulation(examreg.toJson)
      replyTo ! StatusReply.success(Accepted.default)

    case CloseExamregHyperledger(name, replyTo) =>
      connection.closeExaminationRegulation(name)
      replyTo ! StatusReply.success(Accepted.default)

  }

  /** The companion object */
  override val companionObject: HyperledgerActorObject = ExamregHyperledgerBehaviour
}
object ExamregHyperledgerBehaviour extends HyperledgerActorObject {
  /** The EntityTypeKey of this actor */
  override val typeKey: EntityTypeKey[HyperledgerBaseCommand] = EntityTypeKey[HyperledgerBaseCommand](ExamregApplication.hlOffset)
  /** The reference to the entity */
  override val entityId: String = "uc4examreg"
}

