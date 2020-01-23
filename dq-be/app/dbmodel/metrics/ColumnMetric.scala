package dbmodel.metrics

import dbmodel.AppDB
import dbmodel.ModelUtils._
import dbmodel.metrics.Metric.MetricType
import org.squeryl.KeyedEntity
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.annotations.Column

import scala.util.Try

/**
  * Created by Egor Makhov on 28/08/2017.
  */
object ColumnMetric {

  def applyWithMetric(id: String,
                      name: String,
                      description: String,
                      file: String,
                      columns: Seq[String],
                      rawParams: Seq[(String, String)]): (Metric, ColumnMetric, Seq[MetricParameter]) = {
    // TODO: Fix it. Quite unsafe
    val columnStr = toSeparatedString(columns).get
    val params: Seq[MetricParameter] = rawParams.map{
      case (n,v) => MetricParameter(id, n, v)
    }
    (new Metric(id, name, MetricType.column.toString, description), new ColumnMetric(id, file, columnStr), params)
  }

  def unapplyWithMetric(tuple: (Metric, ColumnMetric, Seq[MetricParameter])): Option[(String, String, String, String, Seq[String], Seq[(String, String)])] = {
    val columnSeq = parseSeparatedString(Option(tuple._2.columns))
    val rawParams = tuple._3.map(p => (p.name, p.value))
    Try((tuple._1.id, tuple._1.name, tuple._1.description, tuple._2.source, columnSeq, rawParams)).toOption
  }

  def getById(id: String): (Metric, ColumnMetric) = {
    join(AppDB.columnMetricsTable, AppDB.metricsTable) ((fm, metric) =>
      where(fm.id === id)
        select (metric, fm)
        on(fm.id === metric.id)
    ).single
  }

  def tupleToMap(bundle: Metric Tuple2 ColumnMetric): Map[String, Object] = {
    val columns = parseSeparatedString(Option(bundle._2.columns))
    Map(
      "id"->bundle._1.id,
      "name"->bundle._1.name,
      "description"->bundle._1.description,
      "source"->bundle._2.source,
      "columns"->columns,
      "parameters"->bundle._2.parameters
    )
  }

  def deleteById(id: String): Int = {
    MetricParameter.deleteByOwner(id)
    AppDB.columnMetricsTable.deleteWhere(met => met.id === id)
    Metric.deleteById(id)
  }

  def updateReferencesById(old: String, nuovo: String): Unit = {
    update(AppDB.metricParametersTable)(ff =>
      where(ff.owner === old)
        set(ff.owner := nuovo)
    )
    Metric.updateReferenceById(old, nuovo)
  }

}

case class ColumnMetric (
                        id: String,
                        source: String,
                        @Column("column_names")
                        columns: String
                      )
  extends KeyedEntity[String] {
  // keep in mind, that it should be in transaction
  def insert(): ColumnMetric = AppDB.columnMetricsTable.insert(this)
  def update(): ColumnMetric = {
    AppDB.columnMetricsTable.update(this)
    this
  }
  def rebase(id: String): ColumnMetric = {
    this.insert()
    ColumnMetric.updateReferencesById(id, this.id)
    ColumnMetric.deleteById(id)
    this
  }

  lazy val parameters: Iterator[MetricParameter] =
    AppDB.metricToParameters.left(this).iterator
}