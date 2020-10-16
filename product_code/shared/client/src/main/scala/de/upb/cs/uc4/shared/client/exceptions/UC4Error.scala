package de.upb.cs.uc4.shared.client.exceptions

import de.upb.cs.uc4.shared.client.exceptions.ErrorType.ErrorType
import play.api.http.{ ContentTypeOf, ContentTypes, Writeable }
import play.api.libs.json._
import play.api.mvc.Codec

trait UC4Error {
  val `type`: ErrorType
  val title: String

  override def toString: String = `type`.toString + " : " + title
}

object UC4Error {
  implicit val format: Format[UC4Error] = new Format[UC4Error] {
    override def reads(json: JsValue): JsResult[UC4Error] = json match {
      case json if (json \ "invalidParams").isDefined => Json.fromJson[DetailedError](json)
      case json if (json \ "transactionId").isDefined => Json.fromJson[TransactionError](json)
      case json if (json \ "information").isDefined => Json.fromJson[InformativeError](json)
      case json => Json.fromJson[GenericError](json)
    }

    override def writes(o: UC4Error): JsValue = o match {
      case dErr: DetailedError    => Json.toJson(dErr)
      case gErr: GenericError     => Json.toJson(gErr)
      case tErr: TransactionError => Json.toJson(tErr)
      case iErr: InformativeError => Json.toJson(iErr)
    }
  }

  implicit def jsonWritable[A](implicit writes: Writes[A], codec: Codec): Writeable[A] =
    Writeable(Writeable.writeableOf_JsValue.transform.compose(writes.writes))(ContentTypeOf[A](Some(ContentTypes.JSON)))
}
