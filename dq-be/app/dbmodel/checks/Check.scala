package dbmodel.checks

import dbmodel.AppDB
import dbmodel.checks.Check.CheckType.CheckType
import dbmodel.targets.TargetToChecks
import dbmodel.{BasicEntity, EntityParentEnumeration, SimpleService}
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.annotations.Column
import org.squeryl.{Query, Table}

/**
  * Created by Egor Makhov on 29/08/2017.
  */
object Check extends SimpleService[Check] {

  protected val mainTable: Table[Check] = AppDB.checksTable

  object CheckType extends Enumeration {
    type CheckType = Value
    val snapshot: Check.CheckType.Value = Value("SNAPSHOT")
    val trend: Check.CheckType.Value = Value("TREND")
    val sql: Check.CheckType.Value = Value("SQL")
  }

  object ParentEnum extends EntityParentEnumeration {
    val database = EntityParentVal("database", getIdsByParentDatabase)
    val metric = EntityParentVal("metric", getIdsByParentMetric)

    private def getIdsByParentDatabase(query: String, dbId: String, filter: Option[Enumeration#Value]): Query[String] = {

      filter match {
        case _ => from(AppDB.sqlChecksTable)(sc => where(sc.database === dbId and (sc.id like query)) select sc.id)
      }
    }

    private def getIdsByParentMetric(query: String, metricId: String, filter: Option[Enumeration#Value]): Query[String] = {
      lazy val snapshot: Query[String] = from(AppDB.snapshotChecksTable)(sn => where(sn.metric === metricId and (sn.id like query)) select sn.id)
      lazy val trend: Query[String] = from(AppDB.trendChecksTable)(sn => where(sn.metric === metricId and (sn.id like query)) select sn.id)

      filter match {
        case Some(CheckType.snapshot) => snapshot
        case Some(CheckType.trend) => trend
        case _ => snapshot.union(trend)
      }
    }
  }

  override val typeEnum: Option[Enumeration] = Some(CheckType)
  override val parentEnum: Option[EntityParentEnumeration] = Some(ParentEnum)

  override def searchIdList(query: String, filter: Option[Enumeration#Value] = None, parent: Option[EntityParentEnumeration#EntityParentVal] = None, parentId: Option[String] = None): Query[String] = {
    (filter, parent, parentId) match {
      case (fil, Some(p), Some(pid)) => p.searchF(query, pid, fil)
      case (Some(fil),_,_) => from(mainTable)(t => where(t.id like query and t.getType === fil.toString.toLowerCase) select t.id)
      case (_,_,_) => from(mainTable)(t => where(t.id like query) select t.id)
    }
  }

  override def getIdList(filter: Option[Enumeration#Value] = None): Query[String] = {
    filter match {
      case Some(x: CheckType) => from(mainTable)(chk => where(chk.cType === x.toString.toLowerCase) select chk.id )
      case _ => from(mainTable)(chk => select(chk.id))
    }
  }

  override def getAll(filter: Option[Enumeration#Value] = None): Query[Check] = {
    filter match {
      case Some(x: CheckType) =>
        from(mainTable)(chk => where(chk.cType === x.toString.toLowerCase) select chk )
      case _ => from(mainTable)(chk => select(chk))
    }
  }

  def getChecksByDatabase(db: String): Query[Check] = {
    from(mainTable, AppDB.sqlChecksTable) ((chk, sql) =>
      where(sql.database === db and chk.id === sql.id)
        select chk
    )
  }

  def getChecksByMetric(metricId: String, filter: Option[CheckType] = None): Query[Check] = {
    lazy val snapshot: Query[Check] = join(AppDB.snapshotChecksTable, mainTable)((s, m) => where(s.metric === metricId) select(m) on(s.id === m.id))
    lazy val trend: Query[Check] = join(AppDB.trendChecksTable, mainTable)((t, m) => where(t.metric === metricId) select(m) on(t.id === m.id))

    filter match {
      case Some(x: CheckType) if x == CheckType.snapshot  => snapshot
      case Some(x: CheckType) if x == CheckType.trend => trend
      case _ => snapshot.union(trend)
    }
  }

  def getChecksByMetricIDs(metricIds: List[String], filter: Option[CheckType] = None): Query[Check] = {
    lazy val snapshot: Query[Check] = join(AppDB.snapshotChecksTable, mainTable)((s, m) => where(s.metric in metricIds) select(m) on(s.id === m.id))
    lazy val trend: Query[Check] = join(AppDB.trendChecksTable, mainTable)((t, m) => where(t.metric in metricIds) select(m) on(t.id === m.id))

    filter match {
      case Some(x: CheckType) if x == CheckType.snapshot  => snapshot
      case Some(x: CheckType) if x == CheckType.trend => trend
      case _ => snapshot.union(trend)
    }
  }
  override def deleteById(id: String): Int = {
    TargetToChecks.deleteByCheck(id)
    mainTable.deleteWhere(met => met.id === id)
  }

  override def updateReferenceById(old: String, nuovo: String): Unit = {}

  override def toShortMap(instance: Check): Map[String, Any] = {
    Map("id"-> instance.id, "cType"->instance.cType, "subtype"->instance.subtype, "description"->instance.description)
  }

  def updateByMetric(old: String, nuovo: String): Unit = {
    update(AppDB.snapshotChecksTable)(chk =>
      where(chk.metric === old)
        set(chk.metric := nuovo)
    )
    update(AppDB.trendChecksTable)(chk =>
      where(chk.metric === old)
        set(chk.metric := nuovo)
    )
  }

  def deleteByMetric(metricId: String): Unit = {
    val snapshot = join(AppDB.snapshotChecksTable, mainTable)((s, m) => where(s.metric === metricId) select(m) on(s.id === m.id))
    val trend = join(AppDB.trendChecksTable, mainTable)((t, m) => where(t.metric === metricId) select(m) on(t.id === m.id))

    snapshot.foreach(s => SnapshotCheck.deleteById(s.id))
    trend.foreach(tr => TrendCheck.deleteById(tr.id))
  }
}

case class Check (
                    id: String,
                    @Column("type")
                    cType: String,
                    subtype: String,
                    description: Option[String]
                  )
  extends BasicEntity {
  override def getType: String = cType
  // keep in mind, that it should be in transaction
  def insert(): Check = AppDB.checksTable.insert(this)
  def update(): Check = {
    AppDB.checksTable.update(this)
    this
  }
}
