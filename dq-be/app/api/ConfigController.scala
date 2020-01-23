package api

import com.typesafe.config.{Config, ConfigRenderOptions}
import dbmodel.AppDB
import dbmodel.config.{ConfigReader, ConfigWriter}
import javax.inject.Inject
import org.squeryl.PrimitiveTypeMode.inTransaction
import play.api.libs.Files.TemporaryFile
import play.api.mvc._

import scala.collection.JavaConversions._
/**
  * Created by Egor Makhov on 07/08/2017.
  */
// (cc: ControllerComponents) extends AbstractController(cc)
class ConfigController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  def downloadConfiguration: Action[AnyContent] = Action {
//    try{
      val config = inTransaction(ConfigWriter.generateConfig())
      Ok(config.root().render(ConfigRenderOptions.concise().setFormatted(true).setJson(false)))
        .withHeaders("Content-Disposition" -> "attachment; filename=configuration.conf")
//    } catch {
//      case e: Exception => InternalServerError(e.toString)
//    }
  }
  def getConfiguration(sources:java.util.List[String],virtualFilter:Boolean = true): Action[AnyContent] = Action { request =>
    //implicit lazy val configJsonFormat: Format[Config] = Json.format[Config]

    val config: Config = inTransaction(ConfigWriter.generateConfig(Some(sources.toList),virtualFilter ))
    Ok(config.root().render(ConfigRenderOptions.concise().setFormatted(true).setJson(true)))
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
             case e: Exception => InternalServerError(e.toString)
           }
          case _ => BadRequest("No application/octet-stream files were found!")
        }
      }) getOrElse BadRequest("File missing");
  }

  def resetConfiguration(): Action[AnyContent] = Action {
    inTransaction(AppDB.resetAll())
    Ok
  }

}
