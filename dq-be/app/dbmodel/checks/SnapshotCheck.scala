package dbmodel.checks

import dbmodel.AppDB
import dbmodel.checks.Check.CheckType
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.{KeyedEntity, Query}

import scala.util.Try

/**
  * Created by Egor Makhov on 29/08/2017.
  */
object SnapshotCheck {

  def applyWithCheck(
                      id: String,
                      subtype: String,
                      description: Option[String],
                      metric: String,
                      rawParams: List[(String, String)]): (Check, SnapshotCheck, List[CheckParameter]) = {
    val params: List[CheckParameter] = rawParams.map{
      case (n,v) => CheckParameter(id, n, v)
    }
    (new Check(id, CheckType.snapshot.toString, subtype, description), new SnapshotCheck(id, metric), params)
  }

  def unapplyWithCheck(tuple: (Check, SnapshotCheck, List[CheckParameter])): Option[(String, String, Option[String], String, List[(String, String)])] = {
    val rawParams = tuple._3.map(p => (p.name, p.value))
    Try((tuple._1.id, tuple._1.subtype, tuple._1.description, tuple._2.metric, rawParams)).toOption
  }

  def getById(id: String): Query[(Check, SnapshotCheck)] = {
    join(AppDB.snapshotChecksTable, AppDB.checksTable) ((fm, metric) =>
      where(fm.id === id)
        select (metric, fm)
        on(fm.id === metric.id)
    )
  }

  def tupleToMap(bundle: Check Tuple2 SnapshotCheck): Map[String, Object] = {
    Map(
      "id"->bundle._1.id,
      "subtype"->bundle._1.subtype,
      "description"->bundle._1.description,
      "metric"->bundle._2.metric,
      "parameters"->bundle._2.parameters
    )
  }

  def deleteById(id: String): Int = {
    CheckParameter.deleteByOwner(id)
    AppDB.snapshotChecksTable.deleteWhere(met => met.id === id)
    Check.deleteById(id)
  }

}

case class SnapshotCheck (
                          id: String,
                          metric: String
                        )
  extends KeyedEntity[String] {
  // keep in mind, that it should be in transaction
  def insert(): SnapshotCheck = AppDB.snapshotChecksTable.insert(this)
  def update(): Unit = AppDB.snapshotChecksTable.update(this)

  lazy val parameters: Iterator[CheckParameter] =
    AppDB.snapshotCheckToParameters.left(this).iterator
}
