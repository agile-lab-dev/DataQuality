package api

import com.agilelab.dataquality.api.model._
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json._
import play.api.mvc._
import utils.MyDBSession

@javax.annotation.Generated(value = Array("org.openapitools.codegen.languages.ScalaPlayFrameworkServerCodegen"), date = "2019-11-20T10:48:16.257+01:00[Europe/Rome]")
@Singleton
class SourcesApiController @Inject()(cc: ControllerComponents, api: SourcesApi,session: MyDBSession) extends AbstractController(cc) {

  val logger: Logger = Logger(this.getClass)

  /**
    * GET /dataquality/v1/sources?page=[value]&limit=[value]&sortBy=[value]&orderBy=[value]
    */
  def sourcesList(): Action[AnyContent] = Action { request =>
    implicit val requestId: String = request.id.toString

    logger.info(s"API: ${request.id} - ${request.path}")

    def executeApi(): SourcesResultsResponse = {
      val page = request.getQueryString("page")
        .map(value => value.toInt)
      val limit = request.getQueryString("limit")
        .map(value => value.toInt)
      val sortBy = request.getQueryString("sortBy")
        
      val orderBy = request.getQueryString("orderBy")
        
      api.sourcesList(page, limit, sortBy, orderBy)
    }

    val result = executeApi()
    val json = Json.toJson(result)
    Ok(json)
  }

  def getVirtualsourcesBySourceId(): Action[AnyContent] = Action { request =>
    implicit val requestId: String = request.id.toString

    logger.info(s"API: ${request.id} - ${request.path}")

    def executeApi(): List[String] = {
      api.virtualSourcesIDGet()
    }

    val result = executeApi()
    val json = Json.toJson(result)
    Ok(json)
  }
  /**
    * GET /dataquality/v1/sources/:sourceId
    * @param sourceId The id of the source
    */
  def sourceById(sourceId: String): Action[AnyContent] = Action { request =>
    implicit val requestId: String = request.id.toString

    logger.info(s"API: ${request.id} - ${request.path}")

    def executeApi(): SourceItem = {
      api.sourceById(sourceId)
    }

    val result = executeApi()
    val json = Json.toJson(result)
    Ok(json)
  }

  def getTreeById(sourceId: String): Action[AnyContent] = Action { request =>
    implicit val requestId: String = request.id.toString

    logger.info(s"API: ${request.id} - ${request.path}")

    def executeApi(): (List[SourceItem], List[SourceItem]) = {
      api.getVirtualsourcesBySourceId(sourceId)
    }

    val result = executeApi()
    val json = Json.toJson(result._2.map(s=>s.id))
    Ok(json)
  }

}
