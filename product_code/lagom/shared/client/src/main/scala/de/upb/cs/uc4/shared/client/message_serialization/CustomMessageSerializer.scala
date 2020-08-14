package de.upb.cs.uc4.shared.client.message_serialization

import akka.util.ByteString
import com.lightbend.lagom.scaladsl.api.deser.MessageSerializer.{NegotiatedDeserializer, NegotiatedSerializer}
import com.lightbend.lagom.scaladsl.api.deser.{MessageSerializer, StrictMessageSerializer}
import com.lightbend.lagom.scaladsl.api.transport.{DeserializationException, MessageProtocol, SerializationException}
import de.upb.cs.uc4.shared.client.exceptions.{CustomException, DeserializationError, DetailedError, SimpleError}
import de.upb.cs.uc4.shared.client.exceptions
import play.api.libs.json._

import scala.collection.immutable
import scala.util.control.NonFatal


object CustomMessageSerializer {
  def jsValueFormatMessageSerializer[Message](
                                               implicit jsValueMessageSerializer: MessageSerializer[JsValue, ByteString],
                                               format: Format[Message]
  ): StrictMessageSerializer[Message] = new StrictMessageSerializer[Message] {

    private class JsValueFormatSerializer(jsValueSerializer: NegotiatedSerializer[JsValue, ByteString])
      extends NegotiatedSerializer[Message, ByteString] {
      override def protocol: MessageProtocol = jsValueSerializer.protocol

      override def serialize(message: Message): ByteString = {
        val jsValue =
          try {
            Json.toJson(message)
          } catch {
            case NonFatal(e) =>
              throw SerializationException(e)
          }
        jsValueSerializer.serialize(jsValue)
      }
    }

    private class JsValueFormatDeserializer(jsValueDeserializer: NegotiatedDeserializer[JsValue, ByteString])
      extends NegotiatedDeserializer[Message, ByteString] {
      override def deserialize(wire: ByteString): Message = {
        val jsValue = jsValueDeserializer.deserialize(wire)
        jsValue.validate[Message] match {
          case JsSuccess(message, _) => message
          case JsError(errors) =>
            val errorList: Seq[SimpleError] = errors.map{
              error => SimpleError(error._1.toString().substring(1).replace("/","."),error._2.apply(0).message)
            }.toList
            throw new CustomException(400,exceptions.DetailedError("deserialization error",errorList) )
        }
      }
    }

    override def acceptResponseProtocols: immutable.Seq[MessageProtocol] =
      jsValueMessageSerializer.acceptResponseProtocols

    override def deserializer(protocol: MessageProtocol): NegotiatedDeserializer[Message, ByteString] =
      new JsValueFormatDeserializer(jsValueMessageSerializer.deserializer(protocol))

    override def serializerForResponse(
                                        acceptedMessageProtocols: immutable.Seq[MessageProtocol]
                                      ): NegotiatedSerializer[Message, ByteString] =
      new JsValueFormatSerializer(jsValueMessageSerializer.serializerForResponse(acceptedMessageProtocols))

    override def serializerForRequest: NegotiatedSerializer[Message, ByteString] =
      new JsValueFormatSerializer(jsValueMessageSerializer.serializerForRequest)
  }
}


