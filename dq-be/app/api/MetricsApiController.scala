package api

import com.agilelab.dataquality.api.model._
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json._
import play.api.mvc._
import utils.MyDBSession

@javax.annotation.Generated(value = Array("org.openapitools.codegen.languages.ScalaPlayFrameworkServerCodegen"), date = "2019-11-20T10:48:16.257+01:00[Europe/Rome]")
@Singleton
class MetricsApiController @Inject()(cc: ControllerComponents, api: MetricsApi, session: MyDBSession) extends AbstractController(cc) {

  val logger: Logger = Logger(this.getClass)


  /**
    * GET /dataquality/v1/results/metrics/:metricId?page=[value]&limit=[value]&sortBy=[value]&orderBy=[value]
    *
    * @param metricId The id of the metric
    */
  def metricById(metricId: String): Action[AnyContent] = Action { request =>
    implicit val requestId: String = request.id.toString

    logger.info(s"API: ${request.id} - ${request.path}")

    def executeApi(): MetricsResultsResponse = {
      val page = request.getQueryString("page")
        .map(value => value.toInt)
      val limit = request.getQueryString("limit")
        .map(value => value.toInt)
      val sortBy = request.getQueryString("sortBy")

      val orderBy = request.getQueryString("orderBy")

      api.metricById(metricId, page, limit, sortBy, orderBy)
    }

    val result = executeApi()
    val json = Json.toJson(result)
    Ok(json)
  }

  /**
    * GET /dataquality/v1/results/metrics/source/:sourceId?page=[value]&limit=[value]&sortBy=[value]&orderBy=[value]
    *
    * @param sourceId The id of the source
    */
  def metricsBySourceId(sourceId: String): Action[AnyContent] = Action { request =>
    implicit val requestId: String = request.id.toString

    logger.info(s"API: ${request.id} - ${request.path}")

    def executeApi(): MetricsResultsResponse = {
      val page = request.getQueryString("page")
        .map(value => value.toInt)
      val limit = request.getQueryString("limit")
        .map(value => value.toInt)
      val sortBy = request.getQueryString("sortBy")

      val orderBy = request.getQueryString("orderBy")

      api.metricsBySourceId(sourceId, page, limit, sortBy, orderBy)
    }

    val result = executeApi()
    val json = Json.toJson(result)
    Ok(json)
  }


  def resultsMetricsBySourceIdInRangeInterval(
                                               sourceId: String,
                                               startDate: String,
                                               endDate: String): Action[AnyContent] = Action {
    request =>

      implicit val requestId: String = request.id.toString

      logger.info(s"API: ${request.id} - ${request.path}")

      def executeApi(): MetricsResultsResponse = {
        val page = request.getQueryString("page")
          .map(value => value.toInt)
        val limit = request.getQueryString("limit")
          .map(value => value.toInt)
        val sortBy = request.getQueryString("sortBy")

        val orderBy = request.getQueryString("orderBy")

        api.resultsMetricsBySourceIdInRangeInterval(
          sourceId, startDate, endDate, page, limit, sortBy, orderBy)
      }

      val result = executeApi()
      val json = Json.toJson(result)
      Ok(json)
  }


  /**
    * GET /dataquality/v1/results/metrics/startDate/:startDate/endDate/:endDate?page=[value]&limit=[value]&sortBy=[value]&orderBy=[value]
    *
    * @param startDate pass a startDate as 2019-11-14T00:00 in iso8601
    * @param endDate   pass a endDate as 2019-11-14T00:00 in iso8601
    */
  def resultsMetricsInRangeInterval(startDate: String, endDate: String): Action[AnyContent] = Action { request =>
    implicit val requestId: String = request.id.toString

    logger.info(s"API: ${request.id} - ${request.path}")

    def executeApi(): MetricsResultsResponse = {
      val page = request.getQueryString("page")
        .map(value => value.toInt)
      val limit = request.getQueryString("limit")
        .map(value => value.toInt)
      val sortBy = request.getQueryString("sortBy")

      val orderBy = request.getQueryString("orderBy")

      api.resultsMetricsInRangeInterval(startDate, endDate, page, limit, sortBy, orderBy)
    }

    val result = executeApi()
    val json = Json.toJson(result)
    Ok(json)
  }

}
