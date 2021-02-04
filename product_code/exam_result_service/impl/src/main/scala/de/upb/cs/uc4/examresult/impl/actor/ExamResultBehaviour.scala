package de.upb.cs.uc4.examresult.impl.actor

import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import com.typesafe.config.Config
import de.upb.cs.uc4.hyperledger.commands.{ HyperledgerBaseCommand, HyperledgerCommand }
import de.upb.cs.uc4.hyperledger.connections.cases.ConnectionExamResult
import de.upb.cs.uc4.hyperledger.connections.traits.ConnectionExamResultTrait
import de.upb.cs.uc4.hyperledger.{ HyperledgerActorObject, HyperledgerDefaultActorFactory }

class ExamResultBehaviour(val config: Config) extends HyperledgerDefaultActorFactory[ConnectionExamResultTrait] {

  /** Creates the connection to the chaincode */
  override protected def createConnection: ConnectionExamResultTrait =
    ConnectionExamResult(adminUsername, channel, chaincode, walletPath, networkDescriptionPath)

  override protected def applyCommand(connection: ConnectionExamResultTrait, command: HyperledgerCommand[_]): Unit = ???

  /** The companion object */
  override val companionObject: HyperledgerActorObject = ExamResultBehaviour
}

object ExamResultBehaviour extends HyperledgerActorObject {
  /** The EntityTypeKey of this actor */
  override val typeKey: EntityTypeKey[HyperledgerBaseCommand] = EntityTypeKey[HyperledgerBaseCommand]("uc4exam")
}
