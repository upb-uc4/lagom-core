package de.upb.cs.uc4.operation.model

import de.upb.cs.uc4.operation.model.OperationState.OperationState
import play.api.libs.json.{ Format, Json }

case class OperationData(
    operationId: String,
    transactionInfo: TransactionInfo,
    state: OperationState,
    reason: String,
    existingApprovals: ApprovalList,
    missingApprovals: ApprovalList
)

object OperationData {
  implicit val format: Format[OperationData] = Json.format
}
