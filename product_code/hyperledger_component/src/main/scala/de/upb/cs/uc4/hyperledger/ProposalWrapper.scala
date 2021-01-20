package de.upb.cs.uc4.hyperledger

import de.upb.cs.uc4.hyperledger.HyperledgerUtils.JsonUtil.FromJsonUtil
import de.upb.cs.uc4.shared.client.operation.OperationData
import play.api.libs.json.{ Format, Json }

case class ProposalWrapper(operationId: String, proposal: Array[Byte])

object ProposalWrapper {

  def apply(tuple: (String, Array[Byte])): ProposalWrapper = {
    val (json, proposal) = tuple
    val operationData: OperationData = json.fromJson[OperationData]
    new ProposalWrapper(operationData.operationId, proposal)
  }

  implicit val format: Format[ProposalWrapper] = Json.format
}
