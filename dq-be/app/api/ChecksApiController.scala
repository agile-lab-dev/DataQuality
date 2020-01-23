package api

import com.agilelab.dataquality.api.model._
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json._
import play.api.mvc._
import utils.MyDBSession

@javax.annotation.Generated(value = Array("org.openapitools.codegen.languages.ScalaPlayFrameworkServerCodegen"), date = "2019-11-20T10:48:16.257+01:00[Europe/Rome]")
@Singleton
class ChecksApiController @Inject()(cc: ControllerComponents, api: ChecksApi, session: MyDBSession) extends AbstractController(cc) {

  val logger: Logger = Logger(this.getClass)

  /**
    * GET /dataquality/v1/results/checks/:checkId?page=[value]&limit=[value]&sortBy=[value]&orderBy=[value]
    *
    * @param checkId check id param
    */
  def checkById(checkId: String): Action[AnyContent] = Action { request =>
    implicit val requestId: String = request.id.toString

    logger.info(s"API: ${request.id} - ${request.path}")

    def executeApi(): ChecksResultsResponse = {
      val page = request.getQueryString("page")
        .map(value => value.toInt)
      val limit = request.getQueryString("limit")
        .map(value => value.toInt)
      val sortBy = request.getQueryString("sortBy")

      val orderBy = request.getQueryString("orderBy")

      api.checkById(checkId, page, limit, sortBy, orderBy)
    }

    val result = executeApi()
    val json = Json.toJson(result)
    Ok(json)
  }

  /**
    * GET /dataquality/v1/results/checks/:checkId/startDate/:startDate/endDate/:endDate?page=[value]&limit=[value]&sortBy=[value]&orderBy=[value]
    *
    * @param checkId   check id param
    * @param startDate pass a startDate as 2019-11-14T00:00 in iso8601
    * @param endDate   pass a endDate as 2019-11-14T00:00 in iso8601
    */
  def checkByIdInTimeInterval(checkId: String, startDate: String, endDate: String): Action[AnyContent] = Action { request =>

    implicit val requestId: String = request.id.toString

    logger.info(s"API: ${request.id} - ${request.path}")

    def executeApi(): ChecksResultsResponse = {
      val page = request.getQueryString("page")
        .map(value => value.toInt)
      val limit = request.getQueryString("limit")
        .map(value => value.toInt)
      val sortBy = request.getQueryString("sortBy")

      val orderBy = request.getQueryString("orderBy")

      api.checkByIdInTimeInterval(checkId, startDate, endDate, page, limit, sortBy, orderBy)
    }

    val result = executeApi()
    val json = Json.toJson(result)
    Ok(json)
  }

  /**
    * GET /dataquality/v1/results/checks?page=[value]&limit=[value]&sortBy=[value]&orderBy=[value]
    */
  def getCheckList(): Action[AnyContent] = Action { request =>
    implicit val requestId: String = request.id.toString

    logger.info(s"API: ${request.id} - ${request.path}")

    def executeApi(): ChecksResultsResponse = {
      val page = request.getQueryString("page")
        .map(value => value.toInt)
      val limit = request.getQueryString("limit")
        .map(value => value.toInt)
      val sortBy = request.getQueryString("sortBy")

      val orderBy = request.getQueryString("orderBy")

      api.getCheckList(page, limit, sortBy, orderBy)
    }

    val result = executeApi()
    val json = Json.toJson(result)
    Ok(json)
  }

  /**
    * GET /dataquality/v1/results/checks/metric/:metricId?page=[value]&limit=[value]&sortBy=[value]&orderBy=[value]
    *
    * @param metricId metric id param
    */
  def getCheckListByMetricId(metricId: String): Action[AnyContent] = Action { request =>
    implicit val requestId: String = request.id.toString

    logger.info(s"API: ${request.id} - ${request.path}")

    def executeApi(): ChecksResultsResponse = {
      val page = request.getQueryString("page")
        .map(value => value.toInt)
      val limit = request.getQueryString("limit")
        .map(value => value.toInt)
      val sortBy = request.getQueryString("sortBy")

      val orderBy = request.getQueryString("orderBy")

      api.getCheckListByMetricId(metricId, page, limit, sortBy, orderBy)
    }

    val result = executeApi()
    val json = Json.toJson(result)
    Ok(json)
  }

  /**
    * GET /dataquality/v1/results/checks/source/:sourceId?page=[value]&limit=[value]&sortBy=[value]&orderBy=[value]
    *
    * @param sourceId source id param
    */
  def getCheckListBySourceId(sourceId: String): Action[AnyContent] = Action { request =>
    implicit val requestId: String = request.id.toString

    logger.info(s"API: ${request.id} - ${request.path}")

    def executeApi(): ChecksResultsResponse = {
      val page = request.getQueryString("page")
        .map(value => value.toInt)
      val limit = request.getQueryString("limit")
        .map(value => value.toInt)
      val sortBy = request.getQueryString("sortBy")

      val orderBy = request.getQueryString("orderBy")

      api.getCheckListBySourceId(sourceId, page, limit, sortBy, orderBy)
    }

    val result = executeApi()
    val json = Json.toJson(result)
    Ok(json)
  }

  /**
    * GET /dataquality/v1/results/checks/source/:sourceId/startDate/:startDate/endDate/:endDate?page=[value]&limit=[value]&sortBy=[value]&orderBy=[value]
    *
    * @param sourceId  source id param
    * @param startDate pass a startDate as 2019-11-14T00:00 in iso8601
    * @param endDate   pass a endDate as 2019-11-14T00:00 in iso8601
    */
  def getCheckListBySourceIdInTimeInterval(sourceId: String, startDate: String,
                                           endDate: String): Action[AnyContent] = Action { request =>
    implicit val requestId: String = request.id.toString

    logger.info(s"API: ${request.id} - ${request.path}")

    def executeApi(): ChecksResultsResponse = {
      val page = request.getQueryString("page")
        .map(value => value.toInt)
      val limit = request.getQueryString("limit")
        .map(value => value.toInt)
      val sortBy = request.getQueryString("sortBy")

      val orderBy = request.getQueryString("orderBy")

      api.getCheckListBySourceIdInTimeInterval(sourceId, startDate, endDate, page, limit, sortBy, orderBy)
    }

    val result = executeApi()
    val json = Json.toJson(result)
    Ok(json)
  }


  def getCheckListBySourceIdAndStatusInTimeInterval(sourceId: String, status: String, startDate: String,
                                                    endDate: String): Action[AnyContent] = Action { request =>
    implicit val requestId: String = request.id.toString

    logger.info(s"API: ${request.id} - ${request.path}")

    def executeApi(): ChecksResultsResponse = {
      val page = request.getQueryString("page")
        .map(value => value.toInt)
      val limit = request.getQueryString("limit")
        .map(value => value.toInt)
      val sortBy = request.getQueryString("sortBy")

      val orderBy = request.getQueryString("orderBy")

      api.getCheckListBySourceIdAndStatusInTimeInterval(sourceId, status, startDate, endDate, page, limit, sortBy, orderBy)
    }

    val result = executeApi()
    val json = Json.toJson(result)
    Ok(json)
  }

  /**
    * GET /dataquality/v1/results/checks/startDate/:startDate/endDate/:endDate?page=[value]&limit=[value]&sortBy=[value]&orderBy=[value]
    *
    * @param startDate pass a startDate as 2019-11-14T00:00 in iso8601
    * @param endDate   pass a endDate as 2019-11-14T00:00 in iso8601
    */
  def getCheckListInTimeInterval(startDate: String,
                                 endDate: String): Action[AnyContent] = Action { request =>
    implicit val requestId: String = request.id.toString

    logger.info(s"API: ${request.id} - ${request.path}")

    def executeApi(): ChecksResultsResponse = {
      val page = request.getQueryString("page")
        .map(value => value.toInt)
      val limit = request.getQueryString("limit")
        .map(value => value.toInt)
      val sortBy = request.getQueryString("sortBy")

      val orderBy = request.getQueryString("orderBy")

      api.getCheckListInTimeInterval(startDate, endDate, page, limit, sortBy, orderBy)
    }

    val result = executeApi()
    val json = Json.toJson(result)
    Ok(json)
  }

  /**
    * GET /dataquality/v1/results/checks/status/:status?page=[value]&limit=[value]&sortBy=[value]&orderBy=[value]
    */
  def getCheckListByStatus(status: String): Action[AnyContent] = Action { request =>
    implicit val requestId: String = request.id.toString

    logger.info(s"API: ${request.id} - ${request.path}")

    def executeApi(): ChecksResultsResponse = {
      val page = request.getQueryString("page")
        .map(value => value.toInt)
      val limit = request.getQueryString("limit")
        .map(value => value.toInt)
      val sortBy = request.getQueryString("sortBy")

      val orderBy = request.getQueryString("orderBy")

      api.getCheckListByStatus(status, page, limit, sortBy, orderBy)
    }

    val result = executeApi()
    val json = Json.toJson(result)
    Ok(json)
  }

  /**
    * GET /dataquality/v1/results/checks/status/:status/startDate/:startDate/endDate/:endDate?page=[value]&limit=[value]&sortBy=[value]&orderBy=[value]
    *
    * @param startDate pass a startDate as 2019-11-14T00:00 in iso8601
    * @param endDate   pass a endDate as 2019-11-14T00:00 in iso8601
    */
  def getCheckListByStatusInTimeInterval(status: String, startDate: String, endDate: String): Action[AnyContent] = Action { request =>
    implicit val requestId: String = request.id.toString

    logger.info(s"API: ${request.id} - ${request.path}")

    def executeApi(): ChecksResultsResponse = {
      val page = request.getQueryString("page")
        .map(value => value.toInt)
      val limit = request.getQueryString("limit")
        .map(value => value.toInt)
      val sortBy = request.getQueryString("sortBy")

      val orderBy = request.getQueryString("orderBy")

      api.getCheckListByStatusInTimeInterval(status, startDate, endDate, page, limit, sortBy, orderBy)
    }

    val result = executeApi()
    val json = Json.toJson(result)
    Ok(json)
  }

  /**
    * GET /dataquality/v1/results/checks/tag/:tagId?page=[value]&limit=[value]&sortBy=[value]&orderBy=[value]
    *
    * @param tagId tag id param
    */
  def getCheckListByTagId(tagId: String): Action[AnyContent] = Action { request =>
    implicit val requestId: String = request.id.toString

    logger.info(s"API: ${request.id} - ${request.path}")

    def executeApi(): ChecksResultsResponse = {
      val page = request.getQueryString("page")
        .map(value => value.toInt)
      val limit = request.getQueryString("limit")
        .map(value => value.toInt)
      val sortBy = request.getQueryString("sortBy")

      val orderBy = request.getQueryString("orderBy")

      api.getCheckListByTagId(tagId, page, limit, sortBy, orderBy)
    }

    val result = executeApi()
    val json = Json.toJson(result)
    Ok(json)
  }

  /**
    * GET /dataquality/v1/results/checks/tag/:tagId/startDate/:startDate/endDate/:endDate?page=[value]&limit=[value]&sortBy=[value]&orderBy=[value]
    *
    * @param tagId     tag id param
    * @param startDate pass a startDate as 2019-11-14T00:00 in iso8601
    * @param endDate   pass a endDate as 2019-11-14T00:00 in iso8601
    */
  def getCheckListByTagIdInTimeInterval(tagId: String, startDate: String, endDate: String): Action[AnyContent] = Action { request =>
    implicit val requestId: String = request.id.toString

    logger.info(s"API: ${request.id} - ${request.path}")

    def executeApi(): ChecksResultsResponse = {
      val page = request.getQueryString("page")
        .map(value => value.toInt)
      val limit = request.getQueryString("limit")
        .map(value => value.toInt)
      val sortBy = request.getQueryString("sortBy")

      val orderBy = request.getQueryString("orderBy")

      api.getCheckListByTagIdInTimeInterval(tagId, startDate, endDate, page, limit, sortBy, orderBy)
    }

    val result = executeApi()
    val json = Json.toJson(result)
    Ok(json)
  }

}
