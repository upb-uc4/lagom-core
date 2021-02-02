package de.upb.cs.uc4.exam.impl.actor

import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import com.typesafe.config.Config
import de.upb.cs.uc4.hyperledger.HyperledgerActorObject
import de.upb.cs.uc4.hyperledger.commands.HyperledgerBaseCommand

class ExamBehaviour(val config: Config) {
}

object ExamBehaviour extends HyperledgerActorObject {
  /** The EntityTypeKey of this actor */
  override val typeKey: EntityTypeKey[HyperledgerBaseCommand] = EntityTypeKey[HyperledgerBaseCommand]("uc4exam")
}
