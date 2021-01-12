package de.upb.cs.uc4.operation.model

import de.upb.cs.uc4.operation.model.OperationDataState.OperationState
import play.api.libs.json.{ Format, Json }

case class OperationData(
    operationId: String,
    transactionInfo: TransactionInfo,
    state: OperationState,
    reason: String,
    initiator: String,
    initiatorTimestamp: String,
    lastModifiedTimestamp: String,
    existingApprovals: ApprovalList,
    missingApprovals: ApprovalList
) {
  def containsEnrollmentId(enrollmentId: String): Boolean =
    initiator == enrollmentId || existingApprovals.user.contains(enrollmentId) || missingApprovals.user.contains(enrollmentId)

  def containsGroup(group: String): Boolean =
    existingApprovals.user.contains(group) || missingApprovals.user.contains(group)

  def isInvolved(enrollmentId: String, group: String): Boolean =
    containsEnrollmentId(enrollmentId) || containsGroup(group)
}

object OperationData {
  implicit val format: Format[OperationData] = Json.format
}
