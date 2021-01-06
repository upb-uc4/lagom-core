package de.upb.cs.uc4.operation.model

import play.api.libs.json.{ Format, Json }

object OperationState extends Enumeration {
  type OperationState = Value
  val PENDING, FINISHED, REJECTED = Value

  implicit val format: Format[OperationState] = Json.formatEnum(this)

  def All: Seq[OperationState] = values.toSeq
}
