package dbmodel.checks

import dbmodel.AppDB
import dbmodel.checks.Check.CheckType
import org.squeryl.KeyedEntity
import org.squeryl.PrimitiveTypeMode._

import scala.util.Try

/**
  * Created by Egor Makhov on 29/08/2017.
  */
object TrendCheck {

  def applyWithCheck(
                      id: String,
                      subtype: String,
                      description: Option[String],
                      metric: String,
                      rule: String,
                      rawParams: List[(String, String)]): (Check, TrendCheck, List[CheckParameter]) = {
    val params: List[CheckParameter] = rawParams.map{
      case (n,v) => CheckParameter(id, n, v)
    }
    (new Check(id, CheckType.trend.toString, subtype, description), new TrendCheck(id, metric, rule), params)
  }

  def unapplyWithCheck(tuple: (Check, TrendCheck, List[CheckParameter])): Option[(String, String, Option[String], String, String, List[(String, String)])] = {
    val rawParams = tuple._3.map(p => (p.name, p.value))
    Try((tuple._1.id, tuple._1.subtype, tuple._1.description, tuple._2.metric, tuple._2.rule, rawParams)).toOption
  }

  def getById(id: String): (Check, TrendCheck) = {
    join(AppDB.trendChecksTable, AppDB.checksTable) ((fm, metric) =>
      where(fm.id === id)
        select (metric, fm)
        on(fm.id === metric.id)
    ).head
  }

  def tupleToMap(bundle: Check Tuple2 TrendCheck): Map[String, Object] = {
    Map(
      "id"->bundle._1.id,
      "subtype"->bundle._1.subtype,
      "description"->bundle._1.description,
      "metric"->bundle._2.metric,
      "rule"->bundle._2.rule,
      "parameters"->bundle._2.parameters
    )
  }

  def deleteById(id: String): Int = {
    CheckParameter.deleteByOwner(id)
    AppDB.trendChecksTable.deleteWhere(met => met.id === id)
    Check.deleteById(id)
  }

}

case class TrendCheck (
                           id: String,
                           metric: String,
                           rule: String
                         )
  extends KeyedEntity[String] {
  // keep in mind, that it should be in transaction
  def insert(): TrendCheck = AppDB.trendChecksTable.insert(this)
  def update(): TrendCheck = {
    AppDB.trendChecksTable.update(this)
    this
  }

  lazy val parameters: Iterator[CheckParameter] =
    AppDB.trendCheckToParameters.left(this).iterator
}
