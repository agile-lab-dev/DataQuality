package it.agilelab.bigdata.DataQuality.checks.SnapshotChecks

import it.agilelab.bigdata.DataQuality.checks._
import it.agilelab.bigdata.DataQuality.exceptions.IllegalConstraintResultException
import it.agilelab.bigdata.DataQuality.metrics.MetricResult
import it.agilelab.bigdata.DataQuality.utils.DQSettings

import scala.util.Try

/**
  * Created by Gianvito Siciliano on 09/01/17.
  */
/**
  * Base compare function
  */
abstract class GreaterThanSnapshotCheck extends Check {

  def calculateCheck(base: Double, comparison: Double) = base > comparison

  val subType = "GREATER_THAN"
}

/**
  * Implementation for Metric VS Metric, Metric VS Threshold
  */
/**
  * Performs greater than check between metric and threshold
  * @param id check id
  * @param description description
  * @param metrics list of metrics (current case length = 1)
  * @param threshold required threshold level
  * @param settings dataquality configuration
  */
case class GreaterThanThresholdCheck(
    id: String,
    description: String,
    metrics: Seq[MetricResult],
    threshold: Double
)(implicit settings: DQSettings)
    extends GreaterThanSnapshotCheck {

  override def metricsList: Seq[MetricResult] = metrics

  override def addMetricList(metrics: Seq[MetricResult]): Check = {
    GreaterThanThresholdCheck(id, description, metrics, threshold)
  }

  override def run(): CheckResult = {

    require(metrics.size == 1)

    val metricResult = metrics.head

    val checkStatus = CheckUtil.tryToStatus[Double](
      Try(metricResult.result),
      d => calculateCheck(d, threshold))

    val statusString = checkStatus match {
      case CheckSuccess =>
        s"${metricResult.result} > $threshold"
      case CheckFailure =>
        s"${metricResult.result} <= $threshold (failed: Difference is ${threshold - metricResult.result})"
      case CheckError(throwable) =>
        s"Checking ${metricResult.result} = $threshold error: $throwable"
      case default => throw IllegalConstraintResultException(id)
    }

    val checkMessage = CheckMessageGenerator(metricResult,
                                             threshold,
                                             checkStatus,
                                             statusString,
                                             id,
                                             subType)

    val cr =
      CheckResult(
        this.id,
        subType,
        this.description,
        metricResult.sourceId,
        metricResult.metricId,
        None,
        threshold,
        checkStatus.stringValue,
        checkMessage.message,
        settings.refDateString
      )

    cr
  }

}

/**
  * Performs greater than check between metric and metric
  * @param description description
  * @param metrics list of metrics (current case length = 2)
  * @param settings dataquality configuration
  */
case class GreaterThanMetricCheck(
    id: String,
    description: String,
    metrics: Seq[MetricResult],
    compareMetric: String
)(implicit settings: DQSettings)
    extends GreaterThanSnapshotCheck {

  override def metricsList: Seq[MetricResult] = metrics

  override def addMetricList(metrics: Seq[MetricResult]): Check = {
    GreaterThanMetricCheck(id, description, metrics, compareMetric)
  }

  override def run(): CheckResult = {

    require(metrics.size == 2)

    val compareMetricResult = metrics.filter(_.metricId == compareMetric).head

    val metricResult = metrics.filter(_.metricId != compareMetric).head

    val checkStatus = CheckUtil.tryToStatus[Double](
      Try(metricResult.result),
      d => calculateCheck(d, compareMetricResult.result))

    val statusString = checkStatus match {
      case CheckSuccess =>
        s"${metricResult.result} > ${compareMetricResult.result}"
      case CheckFailure =>
        s"${metricResult.result} <= ${compareMetricResult.result} (failed: Difference is ${compareMetricResult.result - metricResult.result})"
      case CheckError(throwable) =>
        s"Checking ${metricResult.result} = ${compareMetricResult.result} error: $throwable"
      case default => throw IllegalConstraintResultException(id)
    }

    val checkMessage = CheckMessageGenerator(metricResult,
                                             compareMetricResult.result,
                                             checkStatus,
                                             statusString,
                                             id,
                                             subType)

    val cr =
      CheckResult(
        this.id,
        subType,
        this.description,
        metricResult.sourceId,
        metricResult.metricId,
        Option(compareMetricResult.metricId),
        compareMetricResult.result,
        checkStatus.stringValue,
        checkMessage.message,
        settings.refDateString
      )

    cr
  }

}
