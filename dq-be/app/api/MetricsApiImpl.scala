package api

import java.text.SimpleDateFormat

import com.agilelab.dataquality.api.model._
import dbmodel.results.{MetricResultsDAO, MetricResultsItemDB}
import javax.inject.Inject


/**
  * Provides a default implementation for [[MetricsApi]].
  */
class MetricsApiImpl @Inject()(dao: MetricResultsDAO) extends MetricsApi {
  // TODO: move on a common class
  private val ISO_8601_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"

  /**
    * @inheritdoc
    */
  override def metricById(metricId: String,
                          page: Option[Int],
                          limit: Option[Int],
                          sortBy: Option[String],
                          orderBy: Option[String])(implicit requestId: String): MetricsResultsResponse = {

    val r = dao getById(metricId, page, limit, sortBy, orderBy)
    MetricsResultsResponse(Some(r._1.toInt), Some(r._2.map(m => DBtoModel(m))))

  }

  private def DBtoModel(m: MetricResultsItemDB): MetricResultsItem = {
    MetricResultsItem(m.metricId, m.name, m.sourceId, m.result, m.metricType, m.date)
  }

  /**
    * @inheritdoc
    */
  override def metricsBySourceId(sourceId: String,
                                 page: Option[Int],
                                 limit: Option[Int],
                                 sortBy: Option[String],
                                 orderBy: Option[String]
                                              )(implicit requestId: String): MetricsResultsResponse = {

    val r = dao getBySource(sourceId, page, limit, sortBy, orderBy)

    MetricsResultsResponse(Some(r._1.toInt), Some(r._2.map(m => DBtoModel(m))))

  }


  /**
    * @inheritdoc
    */
  override def resultsMetricsBySourceIdInRangeInterval(
                                                                                sourceId: String,
                                                                                startDate: String,
                                                                                endDate: String,
                                                                                page: Option[Int],
                                                                                limit: Option[Int],
                                                                                sortBy: Option[String],
                                                                                orderBy: Option[String])
                                                      (implicit requestId: String): MetricsResultsResponse = {

    val dateFormat = new SimpleDateFormat(ISO_8601_PATTERN)

    val startDateD = dateFormat.parse(startDate)
    val endDateD = dateFormat.parse(endDate)


    val r: (Long, List[MetricResultsItemDB]) = dao.getBySourceAndDate(sourceId, startDateD, endDateD, page, limit, sortBy, orderBy)

    MetricsResultsResponse(Some(r._1.toInt), Some(r._2.map(m => DBtoModel(m))))

  }


  /**
    * @inheritdoc
    */
  override def resultsMetricsInRangeInterval(startDate: String,
                                             endDate: String,
                                             page: Option[Int],
                                             limit: Option[Int],
                                             sortBy: Option[String],
                                             orderBy: Option[String])(implicit requestId: String): MetricsResultsResponse = {
    val dateFormat = new SimpleDateFormat(ISO_8601_PATTERN)

    val startDateD = dateFormat.parse(startDate)
    val endDateD = dateFormat.parse(endDate)


    val r: (Long, List[MetricResultsItemDB]) = dao getByDate(startDateD, endDateD, page, limit, sortBy, orderBy)
    MetricsResultsResponse(Some(r._1.toInt), Some(r._2.map(m => DBtoModel(m))))

  }


}
