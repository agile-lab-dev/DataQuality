package it.agilelab.bigdata.DataQuality.checks.SnapshotChecks

import it.agilelab.bigdata.DataQuality.checks._
import it.agilelab.bigdata.DataQuality.exceptions.IllegalConstraintResultException
import it.agilelab.bigdata.DataQuality.metrics.{ColumnMetricResult, ComposedMetricResult, FileMetricResult, MetricResult}

import scala.util.Try

/**
  * Created by Gianvito Siciliano on 09/01/17.
  */

abstract class GreaterThanSnapshotCheck extends Check {

  def calculateCheck(base:Double, comparison:Double) = base > comparison

  val subType = "GREATER_THAN"
}



case class GreaterThanThresholdCheck(
                                      id: String,
                                      name: String,
                                      description: String,
                                      metrics: Seq[MetricResult],
                                      threshold: Double
                                 ) extends GreaterThanSnapshotCheck {


  override def metricsList: Seq[MetricResult] = metrics

  override def addMetricList(metrics: Seq[MetricResult]): Check = {
    GreaterThanThresholdCheck(id, name, description, metrics, threshold)
  }

  override def run(): CheckResult = {

    require(metrics.size==1) //TODO need just threshold and one column metric as emptyValues, nullValues...

    val metricResult = metrics.head

    val checkStatus = CheckUtil.tryToStatus[Double](Try(metricResult.result), d => calculateCheck(d, threshold))

    val checkMessage = GreaterThanCheckResult(metricResult, threshold, checkStatus, id, subType)

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







case class GreaterThanMetricCheck(
                                   id: String,
                                   name: String,
                                   description: String,
                                   metrics: Seq[MetricResult],
                                   compareMetric: String
                                 ) extends GreaterThanSnapshotCheck {

  override def metricsList: Seq[MetricResult] = metrics

  override def addMetricList(metrics: Seq[MetricResult]): Check = {
    GreaterThanMetricCheck(id, name, description, metrics, compareMetric)
  }

  override def run(): CheckResult = {

    require(metrics.size==2) //TODO need just threshold and one column metric as emptyValues, nullValues...

    val compareMetricResult = metrics.filter(_.id==compareMetric).head

    val metricResult = metrics.filter(_.id!=compareMetric).head

    val checkStatus = CheckUtil.tryToStatus[Double](Try(metricResult.result), d => calculateCheck(d, compareMetricResult.result))

    val checkMessage = GreaterThanCheckResult(metricResult, compareMetricResult.result, checkStatus, id, subType)


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






case class GreaterThanCheckResult(
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
        s"${metricRes.result} > $threshold"
      case CheckFailure =>
        s"${metricRes.result} <= $threshold (failed: ${threshold - metricRes.result})"
      case CheckError(throwable) =>
        s"Checking ${metricRes.result} > $threshold error: $throwable"
      case default => throw IllegalConstraintResultException(checkId)
    }

    s"Metric $metricName $onFile check if (MetricResult) ${metricRes.result} is $checkSubtype $threshold (compareMetric/threshold). \n Result: ${status.stringValue} \nCheckStatus: $checkStatus."

  }

}
