package de.upb.cs.uc4.user.impl

import com.lightbend.lagom.scaladsl.api.transport.RequestHeader
import de.upb.cs.uc4.shared.client.exceptions.CustomException
import play.api.mvc.{ DefaultActionBuilder, Headers, PlayBodyParsers, Results }
import play.api.routing.Router
import play.api.routing.sird._

import scala.concurrent.{ Await, ExecutionContext }
import scala.concurrent.duration._
import scala.util.{ Failure, Success }

class ImageUploadRouter(action: DefaultActionBuilder, parser: PlayBodyParsers, userApplication: UserApplication) {
  private lazy val userService = userApplication.lagomServer.serviceBinding.service.asInstanceOf[UserServiceImpl]

  private implicit val executionContext: ExecutionContext = userApplication.executionContext

  val router: Router = Router.from {
    case PUT(p"/user-management/image") =>
      action(parser.multipartFormData) { request =>

        val serviceRequest = RequestHeader.Default.withHeaders(request.headers.headers)

        val filePath: String = request.body.file("image").get.ref.getAbsolutePath

        val username: String = request.body.dataParts("username").head

        val responseHeaderFuture = userService.setImage(username).invokeWithHeaders(serviceRequest, filePath).map {
          case (header, _) => header
        }

        try {
          Await.ready(responseHeaderFuture, 5.seconds)
        }
        catch {
          case _: Exception => throw CustomException.InternalServerError
        }

        responseHeaderFuture.value match {
          case Some(Success(responseHeader)) => Results.Created.withHeaders(responseHeader.headers.toSeq: _*)
          case Some(Failure(exception)) => throw exception
          case None => throw CustomException.InternalServerError
        }
      }
  }
}
