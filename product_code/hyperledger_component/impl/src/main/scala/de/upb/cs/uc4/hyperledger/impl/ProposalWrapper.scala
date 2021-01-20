package de.upb.cs.uc4.hyperledger.impl

import de.upb.cs.uc4.hyperledger.api.model.operation.OperationData
import de.upb.cs.uc4.shared.client.JsonUtility.FromJsonUtil
import play.api.libs.json.{ Format, Json }

case class ProposalWrapper(operationId: String, proposal: Array[Byte])

object ProposalWrapper {

  def apply(tuple: (String, Array[Byte])): ProposalWrapper = {
    val (json, proposal) = tuple
    val operationData: OperationData = json.fromJson[OperationData]
    new ProposalWrapper(operationData.initiator, proposal)
  }

  implicit val format: Format[ProposalWrapper] = Json.format
}
