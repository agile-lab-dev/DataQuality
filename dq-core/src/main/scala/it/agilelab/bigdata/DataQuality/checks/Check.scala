package it.agilelab.bigdata.DataQuality.checks

import it.agilelab.bigdata.DataQuality.metrics.MetricResult

/**
  * Created by Gianvito Siciliano on 29/12/16.
  *
  * A class representing a list of METRICS that can be applied to a given DataFrame.
  * In order to run the checks, use the `run` method.
  *
  */
trait Check {

  def id: String
  def description: String
  def metricsList: Seq[MetricResult]
  def getDescription = description
  def getMetrics = s"METRICS[${metricsList.map(_.metricId).mkString(", ")}]"
  def getNumMetrics = metricsList.size

  def addMetricList(metrics: Seq[MetricResult]): Check

  /**
    * Run checks and then report to the reporters.
    */
  def run(): CheckResult
}
