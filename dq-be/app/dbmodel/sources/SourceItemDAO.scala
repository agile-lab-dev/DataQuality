package dbmodel.sources

import dbmodel.AppDB
import dbmodel.MyOwnTypeMode._
import dbmodel.sources.VirtualSourceDB
import org.squeryl.dsl.QueryYield
import play.api.Logger
import utils.UtilFrontend

class SourceItemDAO extends SourceItemDAOApi {

  val logger: Logger = Logger(this.getClass)

  override def getAll(page: Option[Int],
                      limit: Option[Int],
                      sortBy: Option[String],
                      orderBy: Option[String])(implicit requestId: String): (Long, List[SourceItemDB]) = {
    inTransaction({
      val total = from(AppDB.SourcesTable)(s => compute(count())).single.measures

      val offset_and_limit = UtilFrontend.calculateOffsetLimit(page, limit, total)

      val res =
        from(AppDB.SourcesTable)(s => selectSorted(s, sortBy, orderBy)).page(offset_and_limit._1, offset_and_limit._2)

      logger.info(s"[$requestId] QUERY: ${res.statement}")
      (total, res.toList)
    })
  }

  override def getVirtualAll(implicit requestId: String): List[String] = {
    inTransaction({
      val total = from(AppDB.SourcesTable)(s => where(s.`type` === "VIRTUAL") compute count()).single.measures

      val res = from(AppDB.SourcesTable)(s => where(s.`type` === "VIRTUAL") select s)

      logger.info(s"[$requestId] QUERY: ${res.statement}")

      val vs = res.toList
      getSourceTreeByListId(vs.flatMap(v => v.id))

    })
  }

  private def allVirtuals(sources: List[String]): List[VirtualSourceDB] = {
    var vsources = inTransaction({
      from(AppDB.virtualSourceTable)(s => where((s.left in sources) or (s.right in sources)) select s).toList
    })
    if (vsources.nonEmpty) vsources = allVirtuals(vsources.map(s => s.id)) ++ vsources

    vsources
  }

  def getTreeByListId(sourceIds: List[String]): List[String] = {
    val sources = inTransaction({
      from(AppDB.SourcesTable)(s => where(s.id in sourceIds) select s).distinct.toList
    })

    val vresult = allVirtuals(sources.map(s => s.id.get))

    (sources.map(s => s.id.get) ++ vresult.flatMap(v => v.right) ++ vresult.map(v => v.left)).distinct
  }

  def getSourceTreeByListId(vsourceIds: List[String]): List[String] = {

    val res = from(AppDB.virtualSourceTable, AppDB.sourceTable)((v, s) =>
      where(v.id in vsourceIds and
        ((s.id === v.left and s.scType <> "VIRTUAL")
          or (v.right === Some(s.id) and s.scType <> "VIRTUAL"))) select s.id).distinct

    vsourceIds ++ res.toList

  }

  override def getById(sourceId: String)(implicit requestId: String): SourceItemDB = {
    inTransaction({
      from(AppDB.SourcesTable)(s => where(s.id === sourceId) select s).single
    })
  }

  def getTreeById(sourceId: String): (List[SourceItemDB], List[VirtualSourceDB]) = {
    val sources = inTransaction({
      from(AppDB.SourcesTable)(s => where(s.id === sourceId) select s).toList
    })

    val vresult: List[VirtualSourceDB] = allVirtuals(sources.map(s => s.id.get))

    (sources, vresult)
  }

  private def selectSorted(
                            m: SourceItemDB,
                            sortBy: Option[String],
                            orderBy: Option[String]
                          ): QueryYield[SourceItemDB] = {
    sortBy match {
      case Some("id") =>
        orderBy.getOrElse("DESC") match {
          case "ASC" => select(m) orderBy (m.id asc)
          case _ => select(m) orderBy (m.id desc)
        }
      case Some("type") =>
        orderBy.getOrElse("DESC") match {
          case "ASC" => select(m) orderBy (m.`type` asc)
          case _ => select(m) orderBy (m.`type` desc)
        }

      case None =>
        orderBy.getOrElse("DESC") match {
          case "ASC" => select(m) orderBy (m.id asc)
          case _ => select(m) orderBy (m.id desc)
        }
    }
  }

}
