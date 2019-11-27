package it.agilelab.bigdata.DataQuality.checks.TrendChecks

import it.agilelab.bigdata.DataQuality.checks._
import it.agilelab.bigdata.DataQuality.exceptions.IllegalConstraintResultException
import it.agilelab.bigdata.DataQuality.metrics.{ColumnMetricResult, MetricResult}
import it.agilelab.bigdata.DataQuality.utils.io.HistoryDBManager
import it.agilelab.bigdata.DataQuality.utils.{DQSettings, mapResToColumnMet}

import scala.util.Try

/**
  * Created by Egor Makhov on 11/05/2017.
  */
case class TopNRankCheck(id: String,
                         description: String,
                         metrics: Seq[MetricResult],
                         rule: String,
                         threshold: Double,
                         timewindow: Int,
                         startDate: Option[String])(
                          implicit sqlWriter: HistoryDBManager,
                          settings: DQSettings)
    extends Check {

  def calculateJaccardDistance(set1: Set[String], set2: Set[String]): Double = {
    1 - set1.intersect(set2).size.toFloat / set1.union(set2).size
  }

  override def metricsList: Seq[MetricResult] = metrics

  override def addMetricList(metrics: Seq[MetricResult]) =
    TopNRankCheck(id,
                  description,
                  metrics,
                  rule,
                  threshold,
                  timewindow,
                  startDate)

  override def run(): CheckResult = {

    val baseMetricResult: MetricResult = metrics.head

    val metricIds: List[String] = metrics.map(x => x.metricId).toList

    val currMetResult = metrics.map(x => x.additionalResult).toSet

    val dbMetResults: Seq[ColumnMetricResult] = startDate match {
      case Some(date) =>
        sqlWriter.loadResults[ColumnMetricResult](metricIds,
                                                  rule,
                                                  timewindow,
                                                  date)(mapResToColumnMet)
      case None =>
        sqlWriter.loadResults[ColumnMetricResult](metricIds, rule, timewindow)(
          mapResToColumnMet)
    }

    val grouped: Iterable[Set[String]] = dbMetResults
      .map(res => Map(res.sourceDate -> Set(res.additionalResult)))
      .reduce((x, y) =>
        x ++ y.map { case (k, v) => k -> (v ++ x.getOrElse(k, Set.empty)) })
      .values

    // todo Longer window calculation
    val dist = calculateJaccardDistance(currMetResult, grouped.head)
    val checkStatus =
      CheckUtil.tryToStatus[Double](Try(dist), d => d <= threshold)

    val statusString = checkStatus match {
      case CheckSuccess =>
        s"$dist <= $threshold"
      case CheckFailure =>
        s"$dist > $threshold (failed: Difference is ${threshold - dist})"
      case CheckError(throwable) =>
        s"Checking ${baseMetricResult.result} = $threshold error: $throwable"
      case default => throw IllegalConstraintResultException(id)
    }

    val checkMessage = CheckMessageGenerator(baseMetricResult,
                                             threshold,
                                             checkStatus,
                                             statusString,
                                             id,
                                             subType,
                                             Some(rule),
                                             Some(timewindow))

    val cr = CheckResult(
      this.id,
      subType,
      this.description,
      baseMetricResult.sourceId,
      baseMetricResult.metricId,
      None,
      threshold,
      checkStatus.stringValue,
      checkMessage.message,
      settings.refDateString
    )

    cr
  }

  val subType = "TOP_N_RANK_CHECK"

}
