package dbmodel.results

import java.util.Date

import dbmodel.sources.MetricItem
import org.squeryl.Query
import org.squeryl.dsl.ast.LogicalBoolean

trait MetricResultDAOApi {

  def selectSortedColumnar(sortBy: Option[String],
                           paramOrderBy: Option[String],
                           whereClause: (MetricItem, MetricResultsColumnarItemDB) => LogicalBoolean)
                          (implicit requestId: String): Query[(MetricItem, MetricResultsColumnarItemDB)]

  def selectSortedFile(sortBy: Option[String],
                       paramOrderBy: Option[String],
                       whereClause: (MetricItem, MetricResultsFileItemDB) => LogicalBoolean)
                      (implicit requestId: String): Query[(MetricItem, MetricResultsFileItemDB)]

  def getById(metricId: String,
              page: Option[Int],
              limit: Option[Int],
              sortBy: Option[String],
              orderBy: Option[String])(implicit requestId: String): (Long, List[MetricResultsItemDB])

  def getBySource(sourceId: String,
                  page: Option[Int],
                  limit: Option[Int],
                  sortBy: Option[String],
                  orderBy: Option[String])(implicit requestId: String): (Long, List[MetricResultsItemDB])

  def getBySourceAndDate(sourceId: String,
                         startDateD: Date,
                         endDateD: Date,
                         page: Option[Int],
                         limit: Option[Int],
                         sortBy: Option[String],
                         orderBy: Option[String])(implicit requestId: String): (Long, List[MetricResultsItemDB])

  def getByDate(startDateD: Date,
                endDateD: Date,
                page: Option[Int],
                limit: Option[Int],
                sortBy: Option[String],
                orderBy: Option[String])(implicit requestId: String): (Long, List[MetricResultsItemDB])
}
