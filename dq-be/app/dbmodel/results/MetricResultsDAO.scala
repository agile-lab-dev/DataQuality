package dbmodel.results


import java.text.SimpleDateFormat
import java.util.Date

import dbmodel.AppDB
import dbmodel.MyOwnTypeMode.{join, where, _}
import dbmodel.sources.MetricItem
import org.squeryl.Query
import org.squeryl.dsl.ast.LogicalBoolean
import play.api.Logger
import play.api.libs.json._
import utils.UtilFrontend


//todo: fix  typeMetric

class MetricResultsDAO extends MetricResultDAOApi {

  implicit lazy val metricResultsItemJsonFormat: Format[MetricResultsItemDB] = Json.format[MetricResultsItemDB]

  private val DBDATE_PATTERN = "yyyy-MM-dd"
  private val EMPTYDATE = "1000-10-10T00:00:00.000z"

  val logger: Logger = Logger(this.getClass)

  override def selectSortedColumnar(sortBy: Option[String],
                                    paramOrderBy: Option[String],
                                    whereClause: (MetricItem, MetricResultsColumnarItemDB) => LogicalBoolean)
                                   (implicit requestId: String): Query[(MetricItem, MetricResultsColumnarItemDB)] = {
    sortBy match {
      case Some("metricId") =>
        paramOrderBy.getOrElse("DESC") match {
          case "ASC" =>
            join(AppDB.MetricItemTable, AppDB.MetricResultsColumnarItemTable)((m, mrc) =>
              select(m, mrc) orderBy (mrc.metricId asc) on (m.id === mrc.metricId and whereClause(m, mrc)))
          case _ =>
            join(AppDB.MetricItemTable, AppDB.MetricResultsColumnarItemTable)((m, mrc) =>
              select(m, mrc) orderBy (mrc.metricId desc) on (m.id === mrc.metricId and whereClause(m, mrc)))
        }
      case Some("sourceId") =>
        paramOrderBy.getOrElse("DESC") match {
          case "ASC" =>
            join(AppDB.MetricItemTable, AppDB.MetricResultsColumnarItemTable)((m, mrc) =>
              select(m, mrc) orderBy (mrc.sourceId asc) on (m.id === mrc.metricId and whereClause(m, mrc)))
          case _ =>
            join(AppDB.MetricItemTable, AppDB.MetricResultsColumnarItemTable)((m, mrc) =>
              select(m, mrc) orderBy (mrc.sourceId desc) on (m.id === mrc.metricId and whereClause(m, mrc)))
        }
      case Some("date") =>
        paramOrderBy.getOrElse("DESC") match {
          case "ASC" =>
            join(AppDB.MetricItemTable, AppDB.MetricResultsColumnarItemTable)((m, mrc) =>
              select(m, mrc) orderBy (mrc.date asc) on (m.id === mrc.metricId and whereClause(m, mrc)))
          case _ =>
            join(AppDB.MetricItemTable, AppDB.MetricResultsColumnarItemTable)((m, mrc) =>
              select(m, mrc) orderBy (mrc.date desc) on (m.id === mrc.metricId and whereClause(m, mrc)))
        }
      case Some("name") =>
        paramOrderBy.getOrElse("DESC") match {
          case "ASC" =>
            join(AppDB.MetricItemTable, AppDB.MetricResultsColumnarItemTable)((m, mrc) =>
              select(m, mrc) orderBy (mrc.name asc) on (m.id === mrc.metricId and whereClause(m, mrc)))
          case _ =>
            join(AppDB.MetricItemTable, AppDB.MetricResultsColumnarItemTable)((m, mrc) =>
              select(m, mrc) orderBy (mrc.name desc) on (m.id === mrc.metricId and whereClause(m, mrc)))
        }
      case Some("result") =>
        paramOrderBy.getOrElse("DESC") match {
          case "ASC" =>
            join(AppDB.MetricItemTable, AppDB.MetricResultsColumnarItemTable)((m, mrc) =>
              select(m, mrc) orderBy (mrc.result asc) on (m.id === mrc.metricId and whereClause(m, mrc)))
          case _ =>
            join(AppDB.MetricItemTable, AppDB.MetricResultsColumnarItemTable)((m, mrc) =>
              select(m, mrc) orderBy (mrc.result desc) on (m.id === mrc.metricId and whereClause(m, mrc)))
        }
      case _ =>
        paramOrderBy.getOrElse("DESC") match {
          case "ASC" =>
            join(AppDB.MetricItemTable, AppDB.MetricResultsColumnarItemTable)((m, mrc) =>
              select(m, mrc) orderBy (mrc.date asc) on (m.id === mrc.metricId and whereClause(m, mrc)))
          case _ =>
            join(AppDB.MetricItemTable, AppDB.MetricResultsColumnarItemTable)((m, mrc) =>
              select(m, mrc) orderBy (mrc.date desc) on (m.id === mrc.metricId and whereClause(m, mrc)))
        }
    }
  }

  override def selectSortedFile(sortBy: Option[String],
                                paramOrderBy: Option[String],
                                whereClause: (MetricItem, MetricResultsFileItemDB) => LogicalBoolean)
                               (implicit requestId: String): Query[(MetricItem, MetricResultsFileItemDB)] = {
    sortBy match {
      case Some("metricId") =>
        paramOrderBy.getOrElse("DESC") match {
          case "ASC" =>
            join(AppDB.MetricItemTable, AppDB.MetricResultsFileItemTable)((m, mrc) =>
              select(m, mrc) orderBy (mrc.metricId asc) on (m.id === mrc.metricId and whereClause(m, mrc)))
          case _ =>
            join(AppDB.MetricItemTable, AppDB.MetricResultsFileItemTable)((m, mrc) =>
              select(m, mrc) orderBy (mrc.metricId desc) on (m.id === mrc.metricId and whereClause(m, mrc)))
        }
      case Some("sourceId") =>
        paramOrderBy.getOrElse("DESC") match {
          case "ASC" =>
            join(AppDB.MetricItemTable, AppDB.MetricResultsFileItemTable)((m, mrc) =>
              select(m, mrc) orderBy (mrc.sourceId asc) on (m.id === mrc.metricId and whereClause(m, mrc)))
          case _ =>
            join(AppDB.MetricItemTable, AppDB.MetricResultsFileItemTable)((m, mrc) =>
              select(m, mrc) orderBy (mrc.sourceId desc) on (m.id === mrc.metricId and whereClause(m, mrc)))
        }
      case Some("date") =>
        paramOrderBy.getOrElse("DESC") match {
          case "ASC" =>
            join(AppDB.MetricItemTable, AppDB.MetricResultsFileItemTable)((m, mrc) =>
              select(m, mrc) orderBy (mrc.date asc) on (m.id === mrc.metricId and whereClause(m, mrc)))
          case _ =>
            join(AppDB.MetricItemTable, AppDB.MetricResultsFileItemTable)((m, mrc) =>
              select(m, mrc) orderBy (mrc.date desc) on (m.id === mrc.metricId and whereClause(m, mrc)))
        }
      case Some("name") =>
        paramOrderBy.getOrElse("DESC") match {
          case "ASC" =>
            join(AppDB.MetricItemTable, AppDB.MetricResultsFileItemTable)((m, mrc) =>
              select(m, mrc) orderBy (mrc.name asc) on (m.id === mrc.metricId and whereClause(m, mrc)))
          case _ =>
            join(AppDB.MetricItemTable, AppDB.MetricResultsFileItemTable)((m, mrc) =>
              select(m, mrc) orderBy (mrc.name desc) on (m.id === mrc.metricId and whereClause(m, mrc)))
        }
      case Some("result") =>
        paramOrderBy.getOrElse("DESC") match {
          case "ASC" =>
            join(AppDB.MetricItemTable, AppDB.MetricResultsFileItemTable)((m, mrc) =>
              select(m, mrc) orderBy (mrc.result asc) on (m.id === mrc.metricId and whereClause(m, mrc)))
          case _ =>
            join(AppDB.MetricItemTable, AppDB.MetricResultsFileItemTable)((m, mrc) =>
              select(m, mrc) orderBy (mrc.result desc) on (m.id === mrc.metricId and whereClause(m, mrc)))
        }
    }
  }

  override def getById(metricId: String,
                       page: Option[Int],
                       limit: Option[Int],
                       sortBy: Option[String],
                       orderBy: Option[String])(implicit requestId: String): (Long, List[MetricResultsItemDB]) = {
    inTransaction {
      val typeMetric = from(AppDB.MetricItemTable)(m => where(m.id === metricId) select m.`type`).singleOption

      typeMetric.flatten match {
        case Some("COLUMN") =>
          val total = join(AppDB.MetricItemTable, AppDB.MetricResultsColumnarItemTable)((m, mrc) =>
            compute(count) on (m.id === mrc.metricId and m.id === metricId)).head.measures
          val offset_and_limit = UtilFrontend.calculateOffsetLimit(page, limit, total)

          val s = selectSortedColumnar(sortBy, orderBy, (m, _) => m.id === metricId)
          logger.info(s"[$requestId] - select sorted columnar: $s")

          (total,
            s.page(offset_and_limit._1, offset_and_limit._2)
              .toList
              .map(m => {
                val mc = m._2
                MetricResultsItemDB(mc.metricId, mc.name, mc.sourceId, mc.result, m._1.`type`, mc.date)
              }))
        case Some("FILE") =>
          val total: Long = join(AppDB.MetricItemTable, AppDB.MetricResultsFileItemTable)((m, mrf) =>
            compute(count()) on (m.id === mrf.metricId and m.id === metricId)).head.measures
          val offset_and_limit = UtilFrontend.calculateOffsetLimit(page, limit, total)

          (total,
            selectSortedFile(sortBy, orderBy, (m, _) => m.id === metricId)
              .page(offset_and_limit._1, offset_and_limit._2)
              .toList
              .map(m => {
                val mc = m._2
                MetricResultsItemDB(mc.metricId, mc.name, mc.sourceId, mc.result, m._1.`type`, mc.date)
              }))
        case _ => (0L, List())

      }
    }

  }

  override def getBySource(sourceId: String,
                           page: Option[Int],
                           limit: Option[Int],
                           sortBy: Option[String],
                           orderBy: Option[String])(implicit requestId: String): (Long, List[MetricResultsItemDB]) = {
    inTransaction {
      //todo: fix it
      val typeMetric: String = "COLUMN"

      val metricById = typeMetric match {
        case "COLUMN" =>
          val total = join(AppDB.MetricItemTable, AppDB.MetricResultsColumnarItemTable)((m, mrc) =>
            compute(count) on (m.id === mrc.metricId and mrc.sourceId === sourceId)).head.measures
          val offset_and_limit = UtilFrontend.calculateOffsetLimit(page, limit, total)

          val s = selectSortedColumnar(sortBy, orderBy, (_, mrc) => mrc.sourceId === sourceId)
          logger.info(s"[$requestId] - select sorted columnar: $s")

          (total,
            s.page(offset_and_limit._1, offset_and_limit._2)
              .toList
              .map(m => {
                val mc = m._2
                MetricResultsItemDB(mc.metricId, mc.name, mc.sourceId, mc.result, m._1.`type`, mc.date)
              }))
        case "FILE" =>
          val total: Long = join(AppDB.MetricItemTable, AppDB.MetricResultsFileItemTable)((m, mrf) =>
            compute(count()) on (m.id === mrf.metricId and mrf.sourceId === sourceId)).head.measures
          val offset_and_limit = UtilFrontend.calculateOffsetLimit(page, limit, total)

          (total,
            selectSortedFile(sortBy, orderBy, (_, mrc) => mrc.sourceId === sourceId)
              .page(offset_and_limit._1, offset_and_limit._2)
              .toList
              .map(m => {
                val mc = m._2
                MetricResultsItemDB(mc.metricId, mc.name, mc.sourceId, mc.result, m._1.`type`, mc.date)
              }))
        case _ => (0L, List())
      }
      metricById
    }
  }


  override def getBySourceAndDate(sourceId: String,
                                  startDateD: Date,
                                  endDateD: Date,
                                  page: Option[Int],
                                  limit: Option[Int],
                                  sortBy: Option[String],
                                  orderBy: Option[String])(implicit requestId: String): (Long, List[MetricResultsItemDB]) = {

    val dateFormatDB = new SimpleDateFormat(DBDATE_PATTERN)

    //todo consider also file
    inTransaction {
      val metrics = {
        val total = join(AppDB.MetricItemTable, AppDB.MetricResultsColumnarItemTable)((m, mrc) =>
          compute(count) on (
            m.id === mrc.metricId
              and mrc.sourceId === sourceId
              and (mrc.date.getOrElse(EMPTYDATE) gte dateFormatDB.format(startDateD))
              and (mrc.date.getOrElse(EMPTYDATE) lt dateFormatDB.format(endDateD))
            )
        ).head.measures

        val offset_and_limit = UtilFrontend.calculateOffsetLimit(page, limit, total)

        val s = selectSortedColumnar(
          sortBy,
          orderBy,
          (_, mrc) =>
            mrc.sourceId === sourceId
              and (mrc.date.getOrElse(EMPTYDATE) gte dateFormatDB.format(startDateD))
              and (mrc.date.getOrElse(EMPTYDATE) lt dateFormatDB.format(endDateD))
        )

        logger.info(s"[$requestId] - select sorted columnar: $s")

        (total,
          s.page(offset_and_limit._1, offset_and_limit._2)
            .toList
            .map(m => {
              val mc = m._2
              MetricResultsItemDB(mc.metricId, mc.name, mc.sourceId, mc.result, m._1.`type`, mc.date)
            }))
      }
      metrics
    }
  }

  override def getByDate(startDateD: Date,
                         endDateD: Date,
                         page: Option[Int],
                         limit: Option[Int],
                         sortBy: Option[String],
                         orderBy: Option[String])(implicit requestId: String): (Long, List[MetricResultsItemDB]) = {
    val dateFormatDB = new SimpleDateFormat(DBDATE_PATTERN)

    inTransaction {
      val typeMetric: String = "COLUMN"

      val metricById = typeMetric match {
        case "COLUMN" =>
          val total = join(AppDB.MetricItemTable, AppDB.MetricResultsColumnarItemTable)(
            (m, mrc) =>
              compute(count) on (m.id === mrc.metricId
                and (mrc.date.getOrElse(EMPTYDATE) gte dateFormatDB.format(startDateD))
                and (mrc.date.getOrElse(EMPTYDATE) lt dateFormatDB.format(endDateD)))
          ).head.measures
          val offset_and_limit = UtilFrontend.calculateOffsetLimit(page, limit, total)

          val s = selectSortedColumnar(
            sortBy,
            orderBy,
            (_, mrc) =>
              (mrc.date.getOrElse(EMPTYDATE) gte dateFormatDB.format(startDateD))
                and (mrc.date.getOrElse(EMPTYDATE) lt dateFormatDB.format(endDateD))
          )

          (total,
            s.page(offset_and_limit._1, offset_and_limit._2)
              .toList
              .map(m => {
                val mc = m._2
                MetricResultsItemDB(mc.metricId, mc.name, mc.sourceId, mc.result, m._1.`type`, mc.date)
              }))
        case "FILE" =>
          val total: Long = join(AppDB.MetricItemTable, AppDB.MetricResultsFileItemTable)(
            (m, mrf) =>
              compute(count()) on (m.id === mrf.metricId and
                (mrf.date.getOrElse(EMPTYDATE) gte dateFormatDB.format(startDateD))
                and (mrf.date.getOrElse(EMPTYDATE) lt dateFormatDB.format(endDateD)))
          ).head.measures
          val offset_and_limit = UtilFrontend.calculateOffsetLimit(page, limit, total)

          (total,
            selectSortedFile(
              sortBy,
              orderBy,
              (_, mrf) =>
                (mrf.date.getOrElse(EMPTYDATE) gte dateFormatDB.format(startDateD))
                  and (mrf.date.getOrElse(EMPTYDATE) lt dateFormatDB.format(endDateD))
            ).page(offset_and_limit._1, offset_and_limit._2)
              .toList
              .map(m => {
                val mc = m._2
                MetricResultsItemDB(mc.metricId, mc.name, mc.sourceId, mc.result, m._1.`type`, mc.date)
              }))
        case _ => (0L, List())

      }
      metricById
    }
  }

}
