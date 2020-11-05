package de.upb.cs.uc4.shared.client

import play.api.libs.json.{ Json, Reads, Writes }

object JsonUtility {
  implicit class ToJsonUtil[Type](val obj: Type)(implicit writes: Writes[Type]) {
    def toJson: String = Json.stringify(Json.toJson(obj))
  }

  implicit class FromJsonUtil(val json: String) {
    def fromJson[Type](implicit reads: Reads[Type]): Type = Json.parse(json).as[Type]
  }
}
