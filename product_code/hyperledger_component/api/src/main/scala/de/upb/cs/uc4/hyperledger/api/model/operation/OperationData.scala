package de.upb.cs.uc4.hyperledger.api.model.operation

import OperationDataState.OperationDataState
import play.api.libs.json.{ Format, Json }

case class OperationData(
    operationId: String,
    transactionInfo: TransactionInfo,
    state: OperationDataState,
    reason: String,
    initiator: String,
    initiatedTimestamp: String,
    lastModifiedTimestamp: String,
    existingApprovals: ApprovalList,
    missingApprovals: ApprovalList
) {
  def containsEnrollmentId(enrollmentId: String): Boolean =
    initiator == enrollmentId || existingApprovals.users.contains(enrollmentId) || missingApprovals.users.contains(enrollmentId)

  def containsGroup(group: String): Boolean =
    existingApprovals.users.contains(group) || missingApprovals.users.contains(group)

  def isInvolved(enrollmentId: String, group: String): Boolean =
    containsEnrollmentId(enrollmentId) || containsGroup(group)
}

object OperationData {
  implicit val format: Format[OperationData] = Json.format
}
