package api

import com.agilelab.dataquality.api.model._

@javax.annotation.Generated(value = Array("org.openapitools.codegen.languages.ScalaPlayFrameworkServerCodegen"), date = "2019-11-20T10:48:16.257+01:00[Europe/Rome]")
trait MetricsApi {
  /**
    * Retrieve the metric
    * Passing a metricId you receive the metric associated
    *
    * @param metricId The id of the metric
    * @param page     The current page, with this the backend calculate the offset
    * @param limit    The numbers of items to return
    * @param sortBy   the field to sort by
    * @param orderBy  can be DESC or ASC
    */
  def metricById(metricId: String, page: Option[Int], limit: Option[Int], sortBy: Option[String],
                 orderBy: Option[String])(implicit requestId: String): MetricsResultsResponse

  /**
    * Retrieve all metrics for a given source
    * Passing a source id you receive all the metrics associated
    *
    * @param sourceId The id of the source
    * @param page     The current page, with this the backend calculate the offset
    * @param limit    The numbers of items to return
    * @param sortBy   the field to sort by
    * @param orderBy  can be DESC or ASC
    */
  def metricsBySourceId(sourceId: String, page: Option[Int], limit: Option[Int], sortBy: Option[String],
                        orderBy: Option[String])(implicit requestId: String): MetricsResultsResponse


  def resultsMetricsBySourceIdInRangeInterval(sourceId: String,
                                              startDate: String,
                                              endDate: String,
                                              page: Option[Int],
                                              limit: Option[Int],
                                              sortBy: Option[String],
                                              orderBy: Option[String])
                                             (implicit requestId: String): MetricsResultsResponse

  /**
    * Retrieve all metrics in a time interval
    * List of metrics result in a range of date
    *
    * @param startDate pass a startDate as 2019-11-14T00:00 in iso8601
    * @param endDate   pass a endDate as 2019-11-14T00:00 in iso8601
    * @param page      The current page, with this the backend calculate the offset
    * @param limit     The numbers of items to return
    * @param sortBy    the field to sort by
    * @param orderBy   can be DESC or ASC
    */
  def resultsMetricsInRangeInterval(startDate: String, endDate: String,
                                    page: Option[Int], limit: Option[Int],
                                    sortBy: Option[String],
                                    orderBy: Option[String])
                                   (implicit requestId: String): MetricsResultsResponse
}


