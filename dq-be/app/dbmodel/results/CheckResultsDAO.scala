package dbmodel.results

import java.text.SimpleDateFormat
import java.util.Date

import dbmodel.MyOwnTypeMode._
import dbmodel._
import dbmodel.sources.{TagCheckItem, TagItem}
import org.squeryl.dsl.QueryYield
import org.squeryl.dsl.ast._
import play.api.Logger
import play.api.libs.json.{Format, Reads, Writes}
import utils.UtilFrontend

class CheckResultsDAO extends CheckResultDAOApi {

  private val DBDATE_PATTERN = "yyyy-MM-dd"
  private val EMPTYDATE = "1000-10-10T00:00:00.000z"
  private val dateFormatDB = new SimpleDateFormat(DBDATE_PATTERN)

  val logger: Logger = Logger(this.getClass)

  def desc(node: ExpressionNode): ExpressionNode = new OrderByArg(node) {
    desc
  }

  def asc(node: ExpressionNode): ExpressionNode = new OrderByArg(node) {
    desc
  }

  // noinspection TypeAnnotation
  object Status extends Enumeration {
    val Success = Value("Success")
    val Failure = Value("Failure")

    type Status = Value
    implicit lazy val StatusJsonFormat: Format[Value] =
      Format(Reads.enumNameReads(this), Writes.enumNameWrites[this.type])
  }


  private def selectSorted(
                            chk: CheckResultsItemDB,
                            sortBy: Option[String],
                            orderBy: Option[String],
                            whereClause: CheckResultsItemDB => LogicalBoolean = _ => TrueLogicalBoolean): QueryYield[CheckResultsItemDB] = {
    sortBy match {
      case Some("checkId") =>
        orderBy.getOrElse("DESC") match {
          case "ASC" => where(whereClause(chk)) select chk orderBy (chk.checkId asc)
          case _ => where(whereClause(chk)) select chk orderBy (chk.checkId desc)
        }
      case Some("checkName") =>
        orderBy.getOrElse("DESC") match {
          case "ASC" => where(whereClause(chk)) select chk orderBy (chk.checkName asc)
          case _ => where(whereClause(chk)) select chk orderBy (chk.checkName desc)
        }
      case Some("description") =>
        orderBy.getOrElse("DESC") match {
          case "ASC" => where(whereClause(chk)) select chk orderBy (chk.description asc)
          case _ => where(whereClause(chk)) select chk orderBy (chk.description desc)
        }
      case Some("checkedFile") =>
        orderBy.getOrElse("DESC") match {
          case "ASC" => where(whereClause(chk)) select chk orderBy (chk.checkedFile asc)
          case _ => where(whereClause(chk)) select chk orderBy (chk.checkedFile desc)
        }
      case Some("baseMetric") =>
        orderBy.getOrElse("DESC") match {
          case "ASC" => where(whereClause(chk)) select chk orderBy (chk.baseMetric asc)
          case _ => where(whereClause(chk)) select chk orderBy (chk.baseMetric desc)
        }
      case Some("comparedMetric") =>
        orderBy.getOrElse("DESC") match {
          case "ASC" => where(whereClause(chk)) select chk orderBy (chk.comparedMetric asc)
          case _ => where(whereClause(chk)) select chk orderBy (chk.comparedMetric desc)
        }
      case Some("comparedThreshold") =>
        orderBy.getOrElse("DESC") match {
          case "ASC" => where(whereClause(chk)) select chk orderBy (chk.comparedThreshold asc)
          case _ => where(whereClause(chk)) select chk orderBy (chk.comparedThreshold desc)
        }
      case Some("status") =>
        orderBy.getOrElse("DESC") match {
          case "ASC" => where(whereClause(chk)) select chk orderBy (chk.status asc)
          case _ => where(whereClause(chk)) select chk orderBy (chk.status desc)
        }
      case Some("message") =>
        orderBy.getOrElse("DESC") match {
          case "ASC" => where(whereClause(chk)) select chk orderBy (chk.message asc)
          case _ => where(whereClause(chk)) select chk orderBy (chk.message desc)
        }
      case Some("execDate") =>
        orderBy.getOrElse("DESC") match {
          case "ASC" => where(whereClause(chk)) select chk orderBy (chk.execDate asc)
          case _ => where(whereClause(chk)) select chk orderBy (chk.execDate desc)
        }
      case None =>
        orderBy.getOrElse("DESC") match {
          case "ASC" => where(whereClause(chk)) select chk orderBy (chk.execDate asc)
          case _ => where(whereClause(chk)) select chk orderBy (chk.execDate desc)
        }
    }
  }

  /**
    *
    * @param checkId check id param
    * @param page    The current page, with this the backend calculate the offset
    * @param limit   The numbers of items to return
    * @param sortBy  the field to sort by
    * @param orderBy can be DESC or ASC
    * @return
    */
  override def getByCheckID(checkId: String,
                            page: Option[Int],
                            limit: Option[Int],
                            sortBy: Option[String],
                            orderBy: Option[String])(implicit requestId: String): (Long, List[CheckResultsItemDB]) = {
    inTransaction({
      val total = from(AppDB.CheckResultsTable)(chk => where(chk.checkId === checkId) compute count()).head.measures

      val offset_and_limit = UtilFrontend.calculateOffsetLimit(page, limit, total)

      val res = from(AppDB.CheckResultsTable)(chk => {
        selectSorted(chk, sortBy, orderBy, c => c.checkId === checkId)
      }).page(offset_and_limit._1, offset_and_limit._2)

      logger.info(s"[$requestId] QUERY: ${res.statement}")

      (total, res.toList)
    })
  }

  override def getByCheckIdAndDate(checkId: String,
                                   page: Option[Int],
                                   limit: Option[Int],
                                   sortBy: Option[String],
                                   orderBy: Option[String],
                                   startDateD: Date,
                                   endDateD: Date)(implicit requestId: String): (Long, List[CheckResultsItemDB]) = {
    val dateFormatDB = new SimpleDateFormat(DBDATE_PATTERN)
    inTransaction({
      val total = from(AppDB.CheckResultsTable)(
        chk =>
          where(
            chk.checkId === checkId
              and (chk.execDate isNotNull)
              and (chk.execDate.getOrElse(EMPTYDATE) gte dateFormatDB.format(startDateD))
              and (chk.execDate.getOrElse(EMPTYDATE) lt dateFormatDB.format(endDateD))) compute count()).head.measures

      val offset_and_limit = UtilFrontend.calculateOffsetLimit(page, limit, total)

      val res = from(AppDB.CheckResultsTable)(chk => {
        selectSorted(
          chk,
          sortBy,
          orderBy,
          c =>
            c.checkId === checkId
              and (chk.execDate isNotNull)
              and (chk.execDate.getOrElse(EMPTYDATE) gte dateFormatDB.format(startDateD))
              and (chk.execDate.getOrElse(EMPTYDATE) lt dateFormatDB.format(endDateD))
        )
      }).page(offset_and_limit._1, offset_and_limit._2)

      logger.info(s"[$requestId] QUERY: ${res.statement}")

      (total, res.toList)
    })
  }

  override def getAll(page: Option[Int], limit: Option[Int], sortBy: Option[String], orderBy: Option[String])(implicit requestId: String): (Long, List[CheckResultsItemDB]) = {
    inTransaction({
      val total = from(AppDB.CheckResultsTable)(_ => compute(count())).head.measures

      val offset_and_limit = UtilFrontend.calculateOffsetLimit(page, limit, total)

      val res = from(AppDB.CheckResultsTable)(chk => {
        selectSorted(chk, sortBy, orderBy)
      })
        .page(offset_and_limit._1, offset_and_limit._2)

      logger.info(s"[$requestId] QUERY: ${res.statement}")
      (total, res.toList)
    })
  }

  override def getByMetric(metricId: String, page: Option[Int], limit: Option[Int], sortBy: Option[String], orderBy: Option[String])(implicit requestId: String): (Long, List[CheckResultsItemDB]) = {
    inTransaction({

      val total = from(AppDB.CheckResultsTable)(chk => where(chk.baseMetric === metricId) compute count()).head.measures
      val offset_and_limit = UtilFrontend.calculateOffsetLimit(page, limit, total)
      val res = from(AppDB.CheckResultsTable)(chk => {
        selectSorted(chk, sortBy, orderBy, c => c.baseMetric === metricId)
      }).page(offset_and_limit._1, offset_and_limit._2)

      logger.info(s"[$requestId] QUERY: ${res.statement}")
      (total, res.toList)
    })
  }


  override def getBySource(page: Option[Int], limit: Option[Int], sortBy: Option[String], orderBy: Option[String], sourceId: Option[String])(implicit requestId: String): (Long, List[CheckResultsItemDB]) = {

    val whereClause: CheckResultsItemDB => LogicalBoolean =
      chk =>
        chk.checkedFile === sourceId

    privGetBySource(page, limit, sortBy, orderBy, whereClause)

  }

  override def getBySourceDate(page: Option[Int], limit: Option[Int], sortBy: Option[String], orderBy: Option[String],
                               sourceId: Option[String], startDateD: Date, endDateD: Date)(implicit requestId: String): (Long, List[CheckResultsItemDB]) = {

    val whereClause: CheckResultsItemDB => LogicalBoolean =
      chk =>
        ((chk.checkedFile === sourceId)
          and chk.execDate.isNotNull
          and (chk.execDate.getOrElse(EMPTYDATE) gte dateFormatDB.format(startDateD))
          and (chk.execDate.getOrElse(EMPTYDATE) lt dateFormatDB.format(endDateD)))

    privGetBySource(page, limit, sortBy, orderBy, whereClause)
  }

  override def getBySourceAndStatusAndDate(page: Option[Int], limit: Option[Int], sortBy: Option[String],
                                           orderBy: Option[String], sourceId: Option[String], status: String,
                                           startDateD: Date, endDateD: Date)(implicit requestId: String): (Long, List[CheckResultsItemDB]) = {

    val whereClause: CheckResultsItemDB => LogicalBoolean =
      chk =>
        (
          (chk.checkedFile === sourceId)
            and (chk.status === status)
            and chk.execDate.isNotNull
            and (chk.execDate.getOrElse(EMPTYDATE) gte dateFormatDB.format(startDateD))
            and (chk.execDate.getOrElse(EMPTYDATE) lt dateFormatDB.format(endDateD)))

    privGetBySource(page, limit, sortBy, orderBy, whereClause)

  }

  private def privGetBySource(page: Option[Int], limit: Option[Int], sortBy: Option[String], orderBy: Option[String],
                              whereClause: CheckResultsItemDB => LogicalBoolean)(implicit requestId: String): (Long, List[CheckResultsItemDB]) = {

    inTransaction({
      val total = from(AppDB.CheckResultsTable)(
        chk =>
          where(whereClause(chk))
            compute count(chk.checkId)).head.measures

      val offset_and_limit = UtilFrontend.calculateOffsetLimit(page, limit, total)

      val res = from(AppDB.CheckResultsTable)(
        chk => {
          selectSorted(chk, sortBy, orderBy, c => whereClause(c))
        }).distinct
        .page(offset_and_limit._1, offset_and_limit._2)

      (total, res.toList)
    })
  }

  override def getCheckByDate(page: Option[Int], limit: Option[Int], sortBy: Option[String],
                              orderBy: Option[String], startDateD: Date, endDateD: Date)(implicit requestId: String): (Long, List[CheckResultsItemDB]) = {

    inTransaction({
      val total = from(AppDB.CheckResultsTable)(
        chk =>
          where(
            (chk.execDate isNotNull) and (chk.execDate.getOrElse(EMPTYDATE) gte dateFormatDB.format(startDateD))
              and (chk.execDate.getOrElse(EMPTYDATE) lt dateFormatDB.format(endDateD))) compute count()).head.measures

      val offset_and_limit = UtilFrontend.calculateOffsetLimit(page, limit, total)

      val res = from(AppDB.CheckResultsTable)(chk => {
        selectSorted(
          chk,
          sortBy,
          orderBy,
          _ =>
            (chk.execDate isNotNull)
              and (chk.execDate.getOrElse(EMPTYDATE) gte dateFormatDB.format(startDateD))
              and (chk.execDate.getOrElse(EMPTYDATE) lt dateFormatDB.format(endDateD))
        )
      }).page(offset_and_limit._1, offset_and_limit._2)

      logger.info(s"[$requestId] QUERY: ${res.statement}")
      (total, res.toList)
    })
  }

  override def getByStatus(status: String, page: Option[Int], limit: Option[Int], sortBy: Option[String],
                           orderBy: Option[String])(implicit requestId: String): (Long, List[CheckResultsItemDB]) = {
    inTransaction({
      val total = from(AppDB.CheckResultsTable)(chk => where(chk.status === status) compute count()).head.measures

      val offset_and_limit = UtilFrontend.calculateOffsetLimit(page, limit, total)

      val res = from(AppDB.CheckResultsTable)(chk => {
        selectSorted(chk, sortBy, orderBy, c => c.status === status)
      }).page(offset_and_limit._1, offset_and_limit._2)

      logger.info(s"[$requestId] QUERY: ${res.statement}")
      (total, res.toList)
    })
  }

  override def getByStatusAndDate(status: String, page: Option[Int], limit: Option[Int], sortBy: Option[String],
                                  orderBy: Option[String], startDateD: Date,
                                  endDateD: Date)(implicit requestId: String): (Long, List[CheckResultsItemDB]) = {
    val dateFormatDB = new SimpleDateFormat(DBDATE_PATTERN)

    inTransaction({
      val total = from(AppDB.CheckResultsTable)(
        chk =>
          where(
            (chk.execDate isNotNull) and chk.status === status
              and (chk.execDate.getOrElse(EMPTYDATE) gte dateFormatDB.format(startDateD))
              and (chk.execDate.getOrElse(EMPTYDATE) lt dateFormatDB.format(endDateD))) compute count()).head.measures

      val offset_and_limit = UtilFrontend.calculateOffsetLimit(page, limit, total)

      val res = from(AppDB.CheckResultsTable)(chk => {
        selectSorted(
          chk,
          sortBy,
          orderBy,
          _ =>
            (chk.execDate isNotNull) and chk.status === status
              and (chk.execDate.getOrElse(EMPTYDATE) gte dateFormatDB.format(startDateD))
              and (chk.execDate.getOrElse(EMPTYDATE) lt dateFormatDB.format(endDateD))
        )
      }).page(offset_and_limit._1, offset_and_limit._2)

      logger.info(s"[$requestId] QUERY: ${res.statement}")
      (total, res.toList)
    })
  }

  override def getChecksByTag(page: Option[Int], limit: Option[Int], sortBy: Option[String],
                              orderBy: Option[String], whereClause: (CheckResultsItemDB, TagCheckItem, TagItem) => LogicalBoolean)(implicit requestId: String): (Long, List[CheckResultsItemDB]) = {
    inTransaction({
      val total = from(AppDB.CheckResultsTable, AppDB.TagChecksTable, AppDB.TagsTable)(
        (chk, tct, tt) =>
          where(whereClause(chk, tct, tt))
            compute count()).head.measures

      val offset_and_limit = UtilFrontend.calculateOffsetLimit(page, limit, total)

      val res = from(AppDB.CheckResultsTable, AppDB.TagChecksTable, AppDB.TagsTable)((chk, tct, tt) => {
        selectSorted(chk, sortBy, orderBy, c => whereClause(c, tct, tt))
      }).distinct
        .page(offset_and_limit._1, offset_and_limit._2)
      logger.info(s"[$requestId] QUERY: ${res.statement}")
      (total, res.toList)
    })
  }

  override def getByTagAndDate(tagId: String, startDateD: Date, endDateD: Date,
                               page: Option[Int], limit: Option[Int], sortBy: Option[String],
                               orderBy: Option[String])(implicit requestId: String): (Long, List[CheckResultsItemDB]) = {
    val dateFormatDB = new SimpleDateFormat(DBDATE_PATTERN)

    val whereClause: (CheckResultsItemDB, TagCheckItem, TagItem) => LogicalBoolean =
      (chk, tct, tt) =>
        ((chk.checkId === tct.check_id) and (tt.tag_id === tct.tag_id and tt.name === tagId)
          and (chk.execDate.getOrElse(EMPTYDATE) gte dateFormatDB.format(startDateD))
          and (chk.execDate.getOrElse(EMPTYDATE) lt dateFormatDB.format(endDateD)))
    getChecksByTag(page, limit, sortBy, orderBy, whereClause)

  }

  override def getByTag(tagId: String, page: Option[Int], limit: Option[Int], sortBy: Option[String],
                        orderBy: Option[String])(implicit requestId: String): (Long, List[CheckResultsItemDB]) = {
    val whereClause: (CheckResultsItemDB, TagCheckItem, TagItem) => LogicalBoolean =
      (chk, tct, tt) => (chk.checkId === tct.check_id) and (tt.tag_id === tct.tag_id and tt.name === tagId) //          and (chk.execDate.getOrElse(EMPTYDATE) gte dateFormatDB.format(startDateD))

    getChecksByTag(page, limit, sortBy, orderBy, whereClause)

  }

}
