package de.upb.cs.uc4.image
import akka.util.ByteString
import com.lightbend.lagom.scaladsl.api.ServiceCall
import de.upb.cs.uc4.image.api.ImageProcessingService

import scala.concurrent.Future

class ImageProcessingServiceStub extends ImageProcessingService {

  override def resize(width: Int): ServiceCall[ByteString, ByteString] = {
    image => Future.successful(image)
  }

  override def rotate(rotate: Int): ServiceCall[ByteString, ByteString] = {
    image => Future.successful(image)
  }

  override def smartCrop(width: Int, height: Int, gravity: String = "smart", stripmeta: Boolean = true): ServiceCall[ByteString, ByteString] = {
    image => Future.successful(image)
  }

  override def thumbnail(width: Int, height: Int): ServiceCall[ByteString, ByteString] = {
    image => Future.successful(image)
  }

  override def convert(`type`: String): ServiceCall[ByteString, ByteString] = {
    image => Future.successful(image)
  }
}
