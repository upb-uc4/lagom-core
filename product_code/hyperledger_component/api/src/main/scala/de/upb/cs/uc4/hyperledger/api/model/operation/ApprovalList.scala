package de.upb.cs.uc4.hyperledger.api.model.operation

import play.api.libs.json.{ Format, Json }

case class ApprovalList(users: Seq[String], groups: Seq[String]) {

  def isEmpty: Boolean = users.isEmpty && groups.isEmpty
}

object ApprovalList {
  implicit val format: Format[ApprovalList] = Json.format
}