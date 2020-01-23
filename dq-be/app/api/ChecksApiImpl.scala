package api

import java.text.SimpleDateFormat

import com.agilelab.dataquality.api.model.{CheckResultsItem, ChecksResultsResponse}
import com.agilelab.dataquality.api.{model => apimodels}
import dbmodel.results.{CheckResultsDAO, CheckResultsItemDB}
import javax.inject.Inject
import play.api.libs.json.{Format, Json}

import scala.language.postfixOps

/**
  * Provides a default implementation for [[ChecksApi]].
  */
class ChecksApiImpl @Inject()(dao: CheckResultsDAO) extends ChecksApi {
  private val ISO_8601_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"


  implicit lazy val checkResultsItemJsonFormat: Format[apimodels.CheckResultsItem] = Json.format[apimodels.CheckResultsItem]

  /**
    *
    * @param checkId check id param
    * @param page    The current page, with this the backend calculate the offset
    * @param limit   The numbers of items to return
    * @param sortBy  the field to sort by
    * @param orderBy can be DESC or ASC
    * @return
    */
  override def checkById(checkId: String,
                         page: Option[Int],
                         limit: Option[Int],
                         sortBy: Option[String],
                         orderBy: Option[String])(implicit requestId: String): apimodels.ChecksResultsResponse = {
    val r: (Long, List[CheckResultsItemDB]) = dao.getByCheckID(checkId, page, limit, sortBy, orderBy)

    ChecksResultsResponse(Some(r._1.toInt), Some(r._2.map(c => fromDBtoModel(c))))

  }

  private def fromDBtoModel(c: CheckResultsItemDB): CheckResultsItem = {
    CheckResultsItem(c.checkId,
      c.checkName,
      c.description,
      c.checkedFile,
      c.baseMetric,
      c.comparedMetric,
      c.comparedThreshold,
      c.status,
      c.message,
      c.execDate)
  }

  /**
    *
    * @param checkId   check id param
    * @param startDate pass a startDate as 2019-11-14T00:00 in iso8601
    * @param endDate   pass a endDate as 2019-11-14T00:00 in iso8601
    * @param page      The current page, with this the backend calculate the offset
    * @param limit     The numbers of items to return
    * @param sortBy    the field to sort by
    * @param orderBy   can be DESC or ASC
    * @return
    */
  override def checkByIdInTimeInterval(
                                        checkId: String,
                                        startDate: String,
                                        endDate: String,
                                        page: Option[Int],
                                        limit: Option[Int],
                                        sortBy: Option[String],
                                        orderBy: Option[String])(implicit requestId: String): ChecksResultsResponse = {
    val dateFormat = new SimpleDateFormat(ISO_8601_PATTERN)


    val startDateD = dateFormat.parse(startDate)
    val endDateD = dateFormat.parse(endDate)

    val r = dao.getByCheckIdAndDate(checkId, page, limit, sortBy, orderBy, startDateD, endDateD)
    ChecksResultsResponse(Some(r._1.toInt), Some(r._2.map(c => fromDBtoModel(c))))

  }


  /**
    * unfiltered check get
    *
    * @param page    The current page, with this the backend calculate the offset
    * @param limit   The numbers of items to return
    * @param sortBy  the field to sort by
    * @param orderBy can be DESC or ASC
    * @return
    */
  override def getCheckList(page: Option[Int],
                            limit: Option[Int],
                            sortBy: Option[String],
                            orderBy: Option[String])(implicit requestId: String): ChecksResultsResponse = {
    val r = dao.getAll(page, limit, sortBy, orderBy)
    ChecksResultsResponse(Some(r._1.toInt), Some(r._2.map(c => fromDBtoModel(c))))
  }


  /**
    * @inheritdoc
    */
  override def getCheckListByMetricId(metricId: String,
                                      page: Option[Int],
                                      limit: Option[Int],
                                      sortBy: Option[String],
                                      orderBy: Option[String])(implicit requestId: String): ChecksResultsResponse = {
    val r = dao.getByMetric(metricId, page, limit, sortBy, orderBy)

    ChecksResultsResponse(Some(r._1.toInt), Some(r._2.map(c => fromDBtoModel(c))))

  }


  /**
    * @inheritdoc
    */
  override def getCheckListBySourceId(sourceId: String,
                                      page: Option[Int],
                                      limit: Option[Int],
                                      sortBy: Option[String],
                                      orderBy: Option[String])(implicit requestId: String): ChecksResultsResponse = {

    val r = dao.getBySource(page, limit, sortBy, orderBy, Some(sourceId))
    ChecksResultsResponse(Some(r._1.toInt), Some(r._2.map(c => fromDBtoModel(c))))
  }

  /**
    * @inheritdoc
    */
  override def getCheckListBySourceIdInTimeInterval(

                                                     sourceId: String,
                                                     startDate: String,
                                                     endDate: String,
                                                     page: Option[Int],
                                                     limit: Option[Int],
                                                     sortBy: Option[String],
                                                     orderBy: Option[String])(implicit requestId: String): ChecksResultsResponse = {

    val dateFormat = new SimpleDateFormat(ISO_8601_PATTERN)
    val dateFormatDB = new SimpleDateFormat(DBDATE_PATTERN)

    val startDateD = dateFormat.parse(startDate)
    val endDateD = dateFormat.parse(endDate)

    val r = dao.getBySourceDate(page, limit, sortBy, orderBy, Some(sourceId), startDateD, endDateD)
    ChecksResultsResponse(Some(r._1.toInt), Some(r._2.map(c => fromDBtoModel(c))))
  }

  override def getCheckListBySourceIdAndStatusInTimeInterval(
                                                              sourceId: String,
                                                              status: String,
                                                              startDate: String,
                                                              endDate: String,
                                                              page: Option[Int],
                                                              limit: Option[Int],
                                                              sortBy: Option[String],
                                                              orderBy: Option[String])(implicit requestId: String): ChecksResultsResponse = {

    val dateFormat = new SimpleDateFormat(ISO_8601_PATTERN)
    val dateFormatDB = new SimpleDateFormat(DBDATE_PATTERN)

    val startDateD = dateFormat.parse(startDate)
    val endDateD = dateFormat.parse(endDate)

    val r = dao.getBySourceAndStatusAndDate(page, limit, sortBy, orderBy, Some(sourceId), status, startDateD, endDateD)
    ChecksResultsResponse(Some(r._1.toInt), Some(r._2.map(c => fromDBtoModel(c))))
  }

  /**
    * @inheritdoc
    */
  override def getCheckListInTimeInterval(startDate: String,
                                          endDate: String,
                                          page: Option[Int],
                                          limit: Option[Int],
                                          sortBy: Option[String],
                                          orderBy: Option[String])(implicit requestId: String): ChecksResultsResponse = {

    val dateFormat = new SimpleDateFormat(ISO_8601_PATTERN)

    val startDateD = dateFormat.parse(startDate)
    val endDateD = dateFormat.parse(endDate)
    val r = dao.getCheckByDate(page, limit, sortBy, orderBy, startDateD, endDateD)
    ChecksResultsResponse(Some(r._1.toInt), Some(r._2.map(c => fromDBtoModel(c))))

  }


  /**
    * @inheritdoc
    */
  override def getCheckListByStatus(status: String,
                                          page: Option[Int],
                                          limit: Option[Int],
                                          sortBy: Option[String],
                                          orderBy: Option[String])(implicit requestId: String): ChecksResultsResponse = {
    val r = dao.getByStatus(status, page, limit, sortBy, orderBy)

    ChecksResultsResponse(Some(r._1.toInt), Some(r._2.map(c => fromDBtoModel(c))))

  }


  val EMPTYDATE = "1000-10-10T00:00:00.000z"
  private val DBDATE_PATTERN = "yyyy-MM-dd"

  /**
    * @inheritdoc
    */
  override def getCheckListByStatusInTimeInterval(
                                                   status: String,
                                                   startDate: String,
                                                   endDate: String,
                                                   page: Option[Int],
                                                   limit: Option[Int],
                                                   sortBy: Option[String],
                                                   orderBy: Option[String])(implicit requestId: String): ChecksResultsResponse = {
    val dateFormat = new SimpleDateFormat(ISO_8601_PATTERN)

    val startDateD = dateFormat.parse(startDate)
    val endDateD = dateFormat.parse(endDate)
    val r = dao.getByStatusAndDate(status, page, limit, sortBy, orderBy, startDateD, endDateD)

    ChecksResultsResponse(Some(r._1.toInt), Some(r._2.map(c => fromDBtoModel(c))))

  }

  /**
    * @inheritdoc
    */
  override def getCheckListByTagId(tagId: String,
                                   page: Option[Int],
                                   limit: Option[Int],
                                   sortBy: Option[String],
                                   orderBy: Option[String])(implicit requestId: String): ChecksResultsResponse = {

    val r: (Long, scala.List[CheckResultsItemDB]) = dao.getByTag(tagId, page, limit, sortBy, orderBy)
    ChecksResultsResponse(Some(r._1.toInt), Some(r._2.map(c => fromDBtoModel(c))))
  }


  /**
    * @inheritdoc
    */
  override def getCheckListByTagIdInTimeInterval(
                                                  tagId: String,
                                                  startDate: String,
                                                  endDate: String,
                                                  page: Option[Int],
                                                  limit: Option[Int],
                                                  sortBy: Option[String],
                                                  orderBy: Option[String])(implicit requestId: String): ChecksResultsResponse = {
    val dateFormat = new SimpleDateFormat(ISO_8601_PATTERN)

    val startDateD = dateFormat.parse(startDate)
    val endDateD = dateFormat.parse(endDate)

    val r: (Long, scala.List[CheckResultsItemDB]) = dao.getByTagAndDate(tagId, startDateD, endDateD, page, limit, sortBy, orderBy)
    ChecksResultsResponse(Some(r._1.toInt), Some(r._2.map(c => fromDBtoModel(c))))
  }


}
