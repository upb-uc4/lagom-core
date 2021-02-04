package de.upb.cs.uc4.exam.impl.actor

import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import com.typesafe.config.Config
import de.upb.cs.uc4.hyperledger.connections.cases.ConnectionExam
import de.upb.cs.uc4.hyperledger.connections.traits.ConnectionExamTrait
import de.upb.cs.uc4.hyperledger.impl.commands.{ HyperledgerBaseCommand, HyperledgerCommand }
import de.upb.cs.uc4.hyperledger.impl.{ HyperledgerActor, HyperledgerActorObject }

class ExamBehaviour(val config: Config) extends HyperledgerActor[ConnectionExamTrait] {

  /** Creates the connection to the chaincode */
  override protected def createConnection: ConnectionExamTrait =
    ConnectionExam(adminUsername, channel, chaincode, walletPath, networkDescriptionPath)

  override protected def applyCommand(connection: ConnectionExamTrait, command: HyperledgerCommand[_]): Unit = ???

  /** The companion object */
  override val companionObject: HyperledgerActorObject = ExamBehaviour
}

object ExamBehaviour extends HyperledgerActorObject {
  /** The EntityTypeKey of this actor */
  override val typeKey: EntityTypeKey[HyperledgerBaseCommand] = EntityTypeKey[HyperledgerBaseCommand]("uc4exam")
}
