package dbmodel.results

import java.util.Date

import dbmodel.sources.{TagCheckItem, TagItem}
import org.squeryl.dsl.ast.LogicalBoolean

trait CheckResultDAOApi {

  /**
    *
    * @param checkId check id param
    * @param page    The current page, with this the backend calculate the offset
    * @param limit   The numbers of items to return
    * @param sortBy  the field to sort by
    * @param orderBy can be DESC or ASC
    * @return
    */
  def getByCheckID(checkId: String,
                   page: Option[Int],
                   limit: Option[Int],
                   sortBy: Option[String],
                   orderBy: Option[String])(implicit requestId: String): (Long, List[CheckResultsItemDB])

  def getByCheckIdAndDate(checkId: String,
                          page: Option[Int],
                          limit: Option[Int],
                          sortBy: Option[String],
                          orderBy: Option[String],
                          startDateD: Date,
                          endDateD: Date)(implicit requestId: String): (Long, List[CheckResultsItemDB])

  def getAll(page: Option[Int],
             limit: Option[Int],
             sortBy: Option[String],
             orderBy: Option[String])(implicit requestId: String): (Long, List[CheckResultsItemDB])

  def getByMetric(metricId: String,
                  page: Option[Int],
                  limit: Option[Int],
                  sortBy: Option[String],
                  orderBy: Option[String])(implicit requestId: String): (Long, List[CheckResultsItemDB])

  def getBySource(page: Option[Int],
                  limit: Option[Int],
                  sortBy: Option[String],
                  orderBy: Option[String],
                  sourceId: Option[String])(implicit requestId: String): (Long, List[CheckResultsItemDB])

  def getBySourceDate(page: Option[Int],
                      limit: Option[Int],
                      sortBy: Option[String],
                      orderBy: Option[String],
                      sourceId: Option[String],
                      startDateD: Date,
                      endDateD: Date)(implicit requestId: String): (Long, List[CheckResultsItemDB])

  def getBySourceAndStatusAndDate(page: Option[Int],
                                  limit: Option[Int],
                                  sortBy: Option[String],
                                  orderBy: Option[String],
                                  sourceId: Option[String],
                                  status: String,
                                  startDateD: Date,
                                  endDateD: Date)(implicit requestId: String): (Long, List[CheckResultsItemDB])

  def getCheckByDate(page: Option[Int],
                     limit: Option[Int],
                     sortBy: Option[String],
                     orderBy: Option[String],
                     startDateD: Date,
                     endDateD: Date)(implicit requestId: String): (Long, List[CheckResultsItemDB])

  def getByStatus(status: String,
                  page: Option[Int],
                  limit: Option[Int],
                  sortBy: Option[String],
                  orderBy: Option[String])(implicit requestId: String): (Long, List[CheckResultsItemDB])

  def getByStatusAndDate(status: String,
                         page: Option[Int],
                         limit: Option[Int],
                         sortBy: Option[String],
                         orderBy: Option[String],
                         startDateD: Date,
                         endDateD: Date)(implicit requestId: String): (Long, List[CheckResultsItemDB])

  def getChecksByTag(
                      page: Option[Int],
                      limit: Option[Int],
                      sortBy: Option[String],
                      orderBy: Option[String],
                      whereClause: (CheckResultsItemDB, TagCheckItem, TagItem) => LogicalBoolean)
                    (implicit requestId: String): (Long, List[CheckResultsItemDB])

  def getByTagAndDate(tagId: String,
                      startDateD: Date,
                      endDateD: Date,
                      page: Option[Int],
                      limit: Option[Int],
                      sortBy: Option[String],
                      orderBy: Option[String])(implicit requestId: String): (Long, List[CheckResultsItemDB])

  def getByTag(tagId: String,
               page: Option[Int],
               limit: Option[Int],
               sortBy: Option[String],
               orderBy: Option[String])(implicit requestId: String): (Long, List[CheckResultsItemDB])
}
