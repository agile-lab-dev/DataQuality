package controllers.config

import javax.inject.Inject
import com.typesafe.config.ConfigRenderOptions
import models.AppDB
import models.config.{ConfigReader, ConfigWriter}
import org.squeryl.PrimitiveTypeMode.inTransaction
import play.api.Logger
import play.api.libs.Files.TemporaryFile
import play.api.mvc.{Action, Controller, MultipartFormData}

/**
  * Created by Egor Makhov on 07/08/2017.
  */
class ConfigController @Inject()() extends Controller {

  def downloadConfiguration = Action {
//    try{
      val config = inTransaction(ConfigWriter.generateConfig)
      Ok(config.root().render(ConfigRenderOptions.concise().setFormatted(true).setJson(false)))
        .withHeaders("Content-Disposition" -> "attachment; filename=configuration.conf")
//    } catch {
//      case e: Exception => InternalServerError(e.toString)
//    }
  }

  def uploadConfiguration(): Action[MultipartFormData[TemporaryFile]] = Action(parse.multipartFormData) {
    implicit request => request.body.file("configuration")
      .map(file => {
        file.contentType match {
          case Some("application/octet-stream") =>
           try {
             inTransaction(AppDB.resetAll())
             ConfigReader.parseConfiguration(file.ref.file)
             Ok
           } catch {

             case e:  Exception => {
               Logger.error("ERROR reading config: " + e.getMessage)
               InternalServerError(e.toString)

             }
           }
          case _ => BadRequest("No application/octet-stream files were found!")
        }
      })  getOrElse BadRequest("File missing");
  }

  def resetConfiguration() = Action {
    inTransaction(AppDB.resetAll())
    Ok
  }

}
