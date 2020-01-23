package dbmodel.metrics

import dbmodel.AppDB
import dbmodel._
import dbmodel.checks.Check
import dbmodel.metrics.Metric.MetricType.MetricType
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.annotations.Column
import org.squeryl.{Query, Table}

/**
  * Created by Egor Makhov on 28/08/2017.
  */
object Metric extends SimpleService[Metric] {

  protected val mainTable: Table[Metric] = AppDB.metricsTable

  object MetricType extends Enumeration {
    type MetricType = Value
    val file = Value("FILE")
    val column = Value("COLUMN")
    val composed = Value("COMPOSED")
  }

  object ParentEnum extends EntityParentEnumeration {
    val source = EntityParentVal("source", getIdsByParentSource)
    val metric = EntityParentVal("metric", getIdsByParentMetric)

    private def getIdsByParentSource(query: String, sourceId: String, filter: Option[Enumeration#Value]): Query[String] = {
      lazy val file: Query[String] = from(AppDB.fileMetricsTable)(fm => where(fm.source === sourceId and (fm.id like query)) select fm.id)
      lazy val column: Query[String] = from(AppDB.columnMetricsTable)(fm => where(fm.source === sourceId and (fm.id like query)) select fm.id)

      filter match {
        case Some(MetricType.file) => file
        case Some(MetricType.column) => column
        case _ => file.union(column)
      }
    }

    private def getIdsByParentMetric(query: String, metricId: String, filter: Option[Enumeration#Value]): Query[String] = {
      lazy val composed: Query[String] =
        from(AppDB.composedMetricConnectionsTable)(con => where(con.formulaMetric === metricId and (con.composedMetric like query)) select con.composedMetric).distinct
      filter match {
        case _ => composed
      }
    }
  }

  override val typeEnum: Option[Enumeration] = Some(MetricType)
  override val parentEnum = Some(ParentEnum)

  override def getIdList(filter: Option[Enumeration#Value] = None): Query[String] = {
    filter match {
      case Some(x: MetricType) =>
        from(mainTable)(mtr => where(mtr.mType === x.toString) select mtr.id)
      case _ => from(mainTable)(mtr => select(mtr.id))
    }
  }

  override def getAll(filter: Option[Enumeration#Value] = None): Query[Metric] = {
    filter match {
      case Some(x: MetricType) =>
        from(mainTable)(mtr => where(mtr.mType === x.toString) select mtr)
      case _ => from(mainTable)(mtr => select(mtr))
    }
  }

  override def deleteById(id: String): Int = {
    Check.deleteByMetric(id)
    AppDB.metricsTable.deleteWhere(met => met.id === id)
  }

  override def updateReferenceById(old: String, nuovo: String): Unit = {
    Check.updateByMetric(old, nuovo)
  }

  override def toShortMap(instance: Metric): Map[String, Any] = {
    Map("id"->instance.id, "name"->instance.name, "mType"->instance.mType, "description"->instance.description)
  }

  def getMetricsBySource(sourceId: String, filter: Option[MetricType] = None): Query[Metric] = {
    lazy val file: Query[Metric] = join(AppDB.fileMetricsTable, mainTable)((f, m) => where(f.source === sourceId) select(m) on(f.id === m.id))
    lazy val column: Query[Metric] = join(AppDB.columnMetricsTable, mainTable)((c, m) => where(c.source === sourceId) select(m) on(c.id === m.id))

    filter match {
      case Some(x: MetricType) if x == MetricType.column  => column
      case Some(x: MetricType) if x == MetricType.file => file
      case _ => file.union(column)
    }
  }
  def getMetricsBySourceIds(sourceIds: List[String], filter: Option[MetricType] = None): Query[Metric] = {
    lazy val file: Query[Metric] = join(AppDB.fileMetricsTable, mainTable)((f, m) => where(f.source in sourceIds) select(m) on(f.id === m.id))
    lazy val column: Query[Metric] = join(AppDB.columnMetricsTable, mainTable)((c, m) => where(c.source in sourceIds) select(m) on(c.id === m.id))

    filter match {
      case Some(x: MetricType) if x == MetricType.column  => column
      case Some(x: MetricType) if x == MetricType.file => file
      case _ => file.union(column)
    }
  }

  def deleteBySource(sourceId: String): Unit = {
    val file: Query[FileMetric] = join(AppDB.fileMetricsTable, mainTable)((f, m) =>
      where(f.source === sourceId)
        select(f) on(f.id === m.id))
    val column: Query[ColumnMetric] = join(AppDB.columnMetricsTable, mainTable)((c, m) =>
      where(c.source === sourceId)
        select(c) on(c.id === m.id))

    file.foreach(f => FileMetric.deleteById(f.id))
    column.foreach(c => ColumnMetric.deleteById(c.id))
  }

  // SPECIAL CASE
  def getTopNList: List[String] = {
    from(mainTable)(t => where(t.name === "TOP_N") select t.id).toList
  }
}

case class Metric (
                    id: String,
                    name: String,
                    @Column("type")
                    mType: String,
                    description: String
                  )
  extends BasicEntity {

  override def getType: String = mType
  // keep in mind, that it should be in transaction
  def insert(): Metric = AppDB.metricsTable.insert(this)
  def update(): Metric = {
    AppDB.metricsTable.update(this)
    this
  }
}

