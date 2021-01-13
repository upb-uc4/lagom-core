package de.upb.cs.uc4.operation.impl.actor

import de.upb.cs.uc4.shared.client.operation.OperationData
import play.api.libs.json.{ Format, Json }

case class OperationDataList(operationDataList: Seq[OperationData])

object OperationDataList {
  implicit val format: Format[OperationDataList] = Json.format
}
