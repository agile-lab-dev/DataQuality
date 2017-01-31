package it.agilelab.bigdata.DataQuality.checks.SnapshotChecks

import it.agilelab.bigdata.DataQuality.checks._
import it.agilelab.bigdata.DataQuality.exceptions.IllegalConstraintResultException
import it.agilelab.bigdata.DataQuality.metrics.{ComposedMetricResult, FileMetricResult, ColumnMetricResult, MetricResult}

import scala.util.Try

/**
  * Created by Gianvito Siciliano on 10/01/17.
  */
abstract class LessThanSnapshotCheck extends Check {

  def calculateCheck(base:Double, comparison:Double) = base < comparison

  val subType = "LESS_THAN"

}



case class LessThanThresholdCheck(
                                      id: String,
                                      name: String,
                                      description: String,
                                      metrics: Seq[MetricResult],
                                      threshold: Double
                                    ) extends LessThanSnapshotCheck {

  override def metricsList: Seq[MetricResult] = metrics

  override def addMetricList(metrics: Seq[MetricResult]): Check = {
    LessThanThresholdCheck(id, name, description, metrics, threshold)
  }

  override def run(): CheckResult = {

    require(metrics.size==1) //TODO need just threshold and one column metric as emptyValues, nullValues...

    val metricResult = metrics.head

    val checkStatus = CheckUtil.tryToStatus[Double](Try(metricResult.result), d => calculateCheck(d, threshold))

    val checkMessage = LessThanCheckMessage(metricResult, threshold, checkStatus, id, subType)

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







case class LessThanMetricCheck(
                                   id: String,
                                   name: String,
                                   description: String,
                                   metrics: Seq[MetricResult],
                                   compareMetric: String
                                 ) extends LessThanSnapshotCheck {

  override def metricsList: Seq[MetricResult] = metrics

  override def addMetricList(metrics: Seq[MetricResult]): Check = {
    LessThanMetricCheck(id, name, description, metrics, compareMetric)
  }

  override def run(): CheckResult = {

    require(metrics.size==2) //TODO need just threshold and one column metric as emptyValues, nullValues...

    println(s"METRIC LIST: ${getMetrics} - COMPARE: ${compareMetric}")
    val compareMetricResult = metrics.filter(_.id==compareMetric).head

    val metricResult = metrics.filter(_.id!=compareMetric).head

    val checkStatus = CheckUtil.tryToStatus[Double](Try(metricResult.result), d => calculateCheck(d, compareMetricResult.result))

    val checkMessage = LessThanCheckMessage(metricResult, compareMetricResult.result, checkStatus, id, subType)

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


case class LessThanCheckMessage(
                                 metricRes: MetricResult,
                                 threshold: Double,
                                 status: CheckStatus,
                                 checkId: String,
                                 checkSubtype: String) extends CheckMessage {

  val message: String = {

    val metricName = metricRes.name

    val onFile = metricRes.getType match {
      case "Column" => {
        val mm = metricRes.asInstanceOf[ColumnMetricResult]
        s"on column ${mm.sourceId}[${mm.columnName}]"
      }
      case "File" => s"on file ${metricRes.asInstanceOf[FileMetricResult].sourceId}"
      case "Composed" => s"on file ${metricRes.asInstanceOf[ComposedMetricResult].sourceId}"
    }


    val checkStatus = status match {
      case CheckSuccess =>
        s"${metricRes.result} < $threshold"
      case CheckFailure =>
        s"${metricRes.result} >= $threshold (failed: ${threshold - metricRes.result})"
      case CheckError(throwable) =>
        s"Checking ${metricRes.result} < $threshold error: $throwable"
      case default => throw IllegalConstraintResultException(checkId)
    }

    s"Metric $metricName $onFile check if (MetricResult) ${metricRes.result} is $checkSubtype $threshold (compareMetric/threshold). \n Result: ${status.stringValue} \nCheckStatus: $checkStatus."

  }

}