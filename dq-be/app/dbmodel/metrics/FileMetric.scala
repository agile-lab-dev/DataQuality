package dbmodel.metrics

import dbmodel.AppDB
import dbmodel.metrics.Metric.MetricType
import org.squeryl.KeyedEntity
import org.squeryl.PrimitiveTypeMode._

import scala.util.Try

/**
  * Created by Egor Makhov on 28/08/2017.
  */
object FileMetric {

  def applyWithMetric(id: String, name: String, description: String, file: String): (Metric, FileMetric) = {
    (new Metric(id, name, MetricType.file.toString, description), new FileMetric(id, file))
  }

  def unapplyWithMetric(tuple: Metric Tuple2 FileMetric): Option[(String, String, String, String)] = {
    Try((tuple._1.id, tuple._1.name, tuple._1.description, tuple._2.source)).toOption
  }

  def getById(id: String): (Metric, FileMetric) = {
    join(AppDB.fileMetricsTable, AppDB.metricsTable) ((fm, metric) =>
      where(fm.id === id)
        select (metric, fm)
        on(fm.id === metric.id)
    ).head
  }

  def tupleToMap(bundle: Metric Tuple2 FileMetric): Map[String, String] = {
    Map(
      "id"->bundle._1.id,
      "name"->bundle._1.name,
      "description"->bundle._1.description,
      "source"->bundle._2.source
    )
  }

  def updateReferencesById(old: String, nuovo: String): Unit = {
    Metric.updateReferenceById(old,nuovo)
  }

  def deleteById(id: String): Int = {
    AppDB.fileMetricsTable.deleteWhere(met => met.id === id)
    Metric.deleteById(id)
  }

}

case class FileMetric (
                       id: String,
                       source: String
                     )
  extends KeyedEntity[String] {
  // keep in mind, that it should be in transaction
  def insert(): FileMetric = AppDB.fileMetricsTable.insert(this)
  def update(): FileMetric = {
    AppDB.fileMetricsTable.update(this)
    this
  }
  def rebase(id: String): FileMetric = {
    this.insert()
    FileMetric.updateReferencesById(id, this.id)
    FileMetric.deleteById(id)
    this
  }
}