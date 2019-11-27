package it.agilelab.bigdata.DataQuality.checks.TrendChecks

import it.agilelab.bigdata.DataQuality.checks._
import it.agilelab.bigdata.DataQuality.exceptions.IllegalParameterException
import it.agilelab.bigdata.DataQuality.metrics.{ColumnMetricResult, ComposedMetricResult, FileMetricResult, MetricResult}
import it.agilelab.bigdata.DataQuality.utils.io.HistoryDBManager
import it.agilelab.bigdata.DataQuality.utils._

import scala.util.Try

/**
  * Created by Egor Makhov on 19/05/2017.
  */
/**
  * This the core for single result metric trend analysis. Implements common functionality.
  *
  * @param id check id
  * @param description check description
  * @param metrics List of metrics (should be one metric)
  * @param rule selection rule (date/record)
  * @param threshold requested threshold
  * @param timewindow result selection timewindow
  * @param startDate start date to select results
  * @param sqlWriter local database manager
  * @param settings dataquality configuration
  */
abstract class TrendCheckCore(id: String,
                              description: String,
                              metrics: Seq[MetricResult],
                              rule: String,
                              threshold: Double,
                              timewindow: Int,
                              startDate: Option[String])(
                               implicit sqlWriter: HistoryDBManager,
                               settings: DQSettings)
    extends Check with Logging {

  // Things to be implemented in the child classes
  def addMetricList(metrics: Seq[MetricResult]): Check
  def calculateCheck(metric: Double, avg: Double, threshold: Double): Boolean
  def calculatePrediction(results: Seq[Double]): Double
  def getStatusString(status: CheckStatus,
                      metric: Double,
                      avg: Double,
                      threshold: Double): String
  val subType: String

  override def metricsList: Seq[MetricResult] = metrics

  /**
    * Calculates basic trend check
    * @return Check result
    */
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
      d => calculateCheck(d, avg, threshold))

    val statusString =
      getStatusString(checkStatus, targetMetricResult.result, avg, threshold)

    val checkMessage = CheckMessageGenerator(targetMetricResult,
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
      Some(targetMetricResult.metricId),
      threshold,
      checkStatus.stringValue,
      checkMessage.message,
      settings.refDateString
    )

    cr
  }
}
