package it.agilelab.bigdata.DataQuality.checks.TrendChecks

import it.agilelab.bigdata.DataQuality.checks._
import it.agilelab.bigdata.DataQuality.exceptions.{IllegalConstraintResultException, IllegalParameterException}
import it.agilelab.bigdata.DataQuality.metrics.{ColumnMetricResult, ComposedMetricResult, FileMetricResult, MetricResult}
import it.agilelab.bigdata.DataQuality.utils.io.HistoryDBManager
import it.agilelab.bigdata.DataQuality.utils.{DQSettings, mapResToColumnMet, mapResToComposedMet, mapResToFileMet}
import scala.util.Try


case class AverageBoundRangeCheck(id: String,
                                  description: String,
                                  metrics: Seq[MetricResult],
                                  rule: String,
                                  thresholdUpper: Double,
                                  thresholdLower: Double,
                                  timewindow: Int,
                                  startDate: Option[String])
                                 (implicit sqlWriter: HistoryDBManager, settings: DQSettings)
  extends Check with AverageCheckDistanceCalculator
{


  override def metricsList: Seq[MetricResult] = metrics

  def calculateCheck(metric: Double, avg: Double, thresholdUp: Double, thresholdDown: Double): Boolean = {
    val upperBound = avg * (1 + thresholdUp)
    val lowerBound = avg * (1 + thresholdDown)

    lowerBound <= metric && metric <= upperBound
  }

  def getStatusString(status: CheckStatus, metric: Double, avg: Double, thresholdUp: Double, thresholdDown: Double
                     ): String = {
    val upperBound = avg * (1 + thresholdUp)
    val lowerBound = avg * (1 + thresholdDown)

    status match {
      case CheckSuccess =>
        s"$lowerBound <= $metric <= $upperBound (with avg=$avg)"
      case CheckFailure =>
        s"$metric not in [$lowerBound,$upperBound] (with avg=$avg)(failed: Should be avg * (1 + lowerBound) <= metricResult <= avg * (1 + upperBound))"
      case CheckError(throwable) =>
        s"Checking $metric error: $throwable"
      case default => throw IllegalConstraintResultException(id)
    }
  }

  override def addMetricList(metrics: Seq[MetricResult]) =
    AverageBoundRangeCheck(id,
      description,
      metrics,
      rule,
      thresholdUpper,
      thresholdLower,
      timewindow,
      startDate)

  override def run(): CheckResult = {

    val baseMetricResult: MetricResult = metrics.head
    val targetMetricResult: MetricResult = metrics.last

    val metricIds: List[String] = List(baseMetricResult.metricId)

    // it will automatically select the correct table to load from, based on the main metric class
    val dbMetResults: Seq[Double] = baseMetricResult match {
      case _: ComposedMetricResult =>
        sqlWriter
          .loadResults(
            metricIds,
            rule,
            timewindow,
            startDate.getOrElse(settings.refDateString))(mapResToComposedMet)
          .map(x => x.result)
      case _: ColumnMetricResult =>
        sqlWriter
          .loadResults(
            metricIds,
            rule,
            timewindow,
            startDate.getOrElse(settings.refDateString))(mapResToColumnMet)
          .map(x => x.result)
      case _: FileMetricResult =>
        sqlWriter
          .loadResults(
            metricIds,
            rule,
            timewindow,
            startDate.getOrElse(settings.refDateString))(mapResToFileMet)
          .map(x => x.result)
      case x => throw IllegalParameterException(x.toString)
    }

    /*
     * in the current state we're assuming that time distance between record is always the same
     * so the prediction in the next record after provided ones
     */
    val avg = calculatePrediction(dbMetResults)

    val checkStatus = CheckUtil.tryToStatus[Double](
      Try(targetMetricResult.result),
      d => calculateCheck(d, avg, thresholdUpper, thresholdLower))

    val statusString =
      getStatusString(checkStatus, targetMetricResult.result, avg, thresholdUpper, thresholdLower)

    val checkMessage = CheckMessageGenerator(targetMetricResult,
      thresholdUpper,
      checkStatus,
      statusString,
      id,
      subType,
      Some(rule),
      Some(timewindow)
    )

    val cr = CheckResult(
      this.id,
      subType,
      this.description,
      baseMetricResult.sourceId,
      baseMetricResult.metricId,
      Some(targetMetricResult.metricId),
      thresholdUpper,
      checkStatus.stringValue,
      checkMessage.message,
      settings.refDateString
    )

    cr
  }


  val subType = "AVERAGE_BOUND_RANGE_CHECK"

}
