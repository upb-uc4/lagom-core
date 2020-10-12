package de.upb.cs.uc4.image.api

import akka.util.ByteString
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{ Descriptor, Service, ServiceCall }

/** The ImageProcessingService interface.
  *
  * This describes everything that Lagom needs to know about this external service.
  */
trait ImageProcessingService extends Service {

  /** resizes the image */
  def resize(width: Int): ServiceCall[ByteString, ByteString]

  /** rotates the image */
  def rotate(rotate: Int): ServiceCall[ByteString, ByteString]

  /** fits the image */
  def fit(width: Int, height: Int): ServiceCall[ByteString, ByteString]

  /** converts the image to a thumbnail */
  def thumbnail(width: Int, height: Int): ServiceCall[ByteString, ByteString]

  /** converts an image */
  def convert(`type`: String): ServiceCall[ByteString, ByteString]

  final override def descriptor: Descriptor = {
    import Service._
    named("imageprocessing")
      .withCalls(
        restCall(Method.POST, "/resize?width", resize _),
        restCall(Method.POST, "/rotate?rotate", rotate _),
        restCall(Method.POST, "/fit?width&height", fit _),
        restCall(Method.POST, "/thumbnail?width&height", thumbnail _),
        restCall(Method.POST, "/convert?type", convert _)
      )
      .withAutoAcl(false)
  }
}

