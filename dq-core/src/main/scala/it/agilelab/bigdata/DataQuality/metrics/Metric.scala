package it.agilelab.bigdata.DataQuality.metrics

/**
  * Created by Gianvito Siciliano on 29/12/16.
  * Representation of different metric
  */
sealed trait Metric {
  def id: String
  def name: String
  def description: String
  def paramMap: Map[String, Any]
}

case class ColumnMetric(
    id: String,
    name: String,
    description: String,
    source: String,
    sourceDate: String,
    columns: Seq[String],
    paramMap: Map[String, Any]
) extends Metric

case class FileMetric(
    id: String,
    name: String,
    description: String,
    source: String,
    sourceDate: String,
    paramMap: Map[String, Any]
) extends Metric

case class ComposedMetric(
    id: String,
    name: String,
    description: String,
    formula: String,
    paramMap: Map[String, Any]
) extends Metric

case class OutputMetric(
    id: String,
    name: String,
    description: String,
    outputMetric: String,
    paramMap: Map[String, Any]
) extends Metric
