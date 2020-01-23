package dbmodel.metrics

import dbmodel.AppDB
import dbmodel.metrics.Metric.MetricType
import org.squeryl.KeyedEntity
import org.squeryl.PrimitiveTypeMode._

import scala.util.Try

/**
  * Created by Egor Makhov on 28/08/2017.
  */
object ComposedMetric {

  def applyWithMetric(id: String, name: String, description: String, query: String): (Metric, ComposedMetric) = {
    (new Metric(id, name, MetricType.composed.toString, description), new ComposedMetric(id, query))
  }

  def unapplyWithMetric(tuple: Metric Tuple2 ComposedMetric): Option[(String, String, String, String)] = {
    Try((tuple._1.id, tuple._1.name, tuple._1.description, tuple._2.formula)).toOption
  }

  def getById(id: String): (Metric, ComposedMetric) = {
    join(AppDB.composedMetricsTable, AppDB.metricsTable) ((fm, metric) =>
      where(fm.id === id)
        select (metric, fm)
        on(fm.id === metric.id)
    ).head
  }

  def tupleToMap(bundle: Metric Tuple2 ComposedMetric): Map[String, String] = {
    Map(
      "id"->bundle._1.id,
      "name"->bundle._1.name,
      "description"->bundle._1.description,
      "formula"->bundle._2.formula
    )
  }

  def updateReferencesById(old: String, nuovo: String): Unit = {
    Metric.updateReferenceById(old, nuovo)
  }

  def deleteById(id: String): Int = {
    AppDB.composedMetricsTable.deleteWhere(met => met.id === id)
    Metric.deleteById(id)
  }

}

case class ComposedMetric (
                       id: String,
                       formula: String
                     )
  extends KeyedEntity[String] {
  // keep in mind, that it should be in transaction
  def insert(): ComposedMetric = AppDB.composedMetricsTable.insert(this)
  def update(): ComposedMetric = {
    AppDB.composedMetricsTable.update(this)
    this
  }
  def rebase(id: String): ComposedMetric = {
    this.insert()
    ComposedMetric.updateReferencesById(id, this.id)
    ComposedMetric.deleteById(id)
    this
  }
}