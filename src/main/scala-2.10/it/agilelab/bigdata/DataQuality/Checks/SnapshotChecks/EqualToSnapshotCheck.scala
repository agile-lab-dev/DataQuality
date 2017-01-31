package it.agilelab.bigdata.DataQuality.checks.SnapshotChecks

import it.agilelab.bigdata.DataQuality.checks._
import it.agilelab.bigdata.DataQuality.exceptions.IllegalConstraintResultException
import it.agilelab.bigdata.DataQuality.metrics.{ComposedMetricResult, FileMetricResult, ColumnMetricResult, MetricResult}

import scala.util.Try

/**
  * Created by Gianvito Siciliano on 10/01/17.
  */
abstract class EqualToSnapshotCheck extends Check {

  def calculateCheck(base:Double, comparison:Double) = base == comparison

  val subType = "EQUAL_TO"
}

case class EqualToThresholdCheck(
                                   id: String,
                                   name: String,
                                   description: String,
                                   metrics: Seq[MetricResult],
                                   threshold: Double
                                 ) extends EqualToSnapshotCheck {

  override def metricsList: Seq[MetricResult] = metrics

  override def addMetricList(metrics: Seq[MetricResult]): Check = {
    EqualToThresholdCheck(id, name, description, metrics, threshold)
  }

  override def run(): CheckResult = {

    require(metrics.size==1)

    val metricResult = metrics.head

    val checkStatus  = CheckUtil.tryToStatus[Double](Try(metricResult.result), d => calculateCheck(d, threshold))

    val checkMessage = EqualToCheckResult(metricResult, threshold, checkStatus, id, subType)


    val cr =
      CheckResult(
        this.id,
        subType,
        this.description,
        metricResult.sourceId,
        metricResult.id,
        None,
        threshold,
        checkStatus.stringValue,
        checkMessage.message
      )

    cr
  }
}







case class EqualToMetricCheck(
                                id: String,
                                name: String,
                                description: String,
                                metrics: Seq[MetricResult],
                                compareMetric: String
                              ) extends EqualToSnapshotCheck {

  override def metricsList: Seq[MetricResult] = metrics

  override def addMetricList(metrics: Seq[MetricResult]): Check = {
    EqualToMetricCheck(id, name, description, metrics, compareMetric)
  }

  override def run(): CheckResult = {

    require(metrics.size==2)

    val compareMetricResult = metrics.filter(_.id==compareMetric).head

    val metricResult = metrics.filter(_.id!=compareMetric).head

    val checkStatus = CheckUtil.tryToStatus[Double](Try(metricResult.result), d => calculateCheck(d, compareMetricResult.result))

    val checkMessage = EqualToCheckResult(metricResult, compareMetricResult.result, checkStatus, id, subType)

    val cr =
      CheckResult(
        this.id,
        subType,
        this.description,
        metricResult.sourceId,
        metricResult.id,
        Option(compareMetricResult.id),
        compareMetricResult.result,
        checkStatus.stringValue,
        checkMessage.message
      )

    cr
  }

}



case class EqualToCheckResult(
                               metricRes: MetricResult,
                               threshold: Double,
                               status: CheckStatus,
                               checkId: String,
                               checkSubtype: String) extends CheckMessage {

  val message: String = {

    val metricName = metricRes.name

    val onFile = metricRes.getType match {
      case "Column"   => {
        val mm = metricRes.asInstanceOf[ColumnMetricResult]
        s"on column ${mm.sourceId}[${mm.columnName}]"
      }
      case "File"     => s"on file ${metricRes.asInstanceOf[FileMetricResult].sourceId}"
      case "Composed" => s"on file ${metricRes.asInstanceOf[ComposedMetricResult].sourceId}"
    }


    val checkStatus = status match {
      case CheckSuccess =>
        s"${metricRes.result} = $threshold"
      case CheckFailure =>
        s"${metricRes.result} != $threshold (failed: ${threshold - metricRes.result})"
      case CheckError(throwable) =>
        s"Checking ${metricRes.result} = $threshold error: $throwable"
      case default => throw IllegalConstraintResultException(checkId)
    }

    s"Metric $metricName $onFile check if (MetricResult) ${metricRes.result} is $checkSubtype $threshold (compareMetric/threshold). \n Result: ${status.stringValue} \nCheckStatus: $checkStatus."

  }

}
