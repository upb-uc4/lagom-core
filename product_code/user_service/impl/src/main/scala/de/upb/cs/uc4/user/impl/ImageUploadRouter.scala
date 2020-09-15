package de.upb.cs.uc4.user.impl

import play.api.mvc.{ DefaultActionBuilder, PlayBodyParsers, Results }
import play.api.routing.Router
import play.api.routing.sird._

class ImageUploadRouter(action: DefaultActionBuilder, parser: PlayBodyParsers) {
  val router: Router = Router.from {
    case PUT(p"/user-management/image") =>
      action(parser.multipartFormData) { request =>
        val filePaths = request.body.files.map(_.ref.getAbsolutePath)
        val data = request.body.dataParts.map(tuple => tuple._1 + " -> " + tuple._2.mkString("[", ",", "]"))
        Results.Ok(filePaths.mkString("Uploaded[", ", ", "]") + "   " + data.mkString("Data[", ", ", "]"))
      }
  }
}
