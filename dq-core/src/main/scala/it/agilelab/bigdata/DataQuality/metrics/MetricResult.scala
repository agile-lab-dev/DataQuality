package it.agilelab.bigdata.DataQuality.metrics

import it.agilelab.bigdata.DataQuality.metrics.DQResultTypes.DQResultType

/**
  * Created by Gianvito Siciliano on 29/12/16.
  *
  * Representations of different metric results
  */
object DQResultTypes extends Enumeration {
  type DQResultType = Value
  val column: DQResultType = Value("Column")
  val composed: DQResultType = Value("Composed")
  val file: DQResultType = Value("File")
  val check: DQResultType = Value("Check")
}

trait TypedResult {
  def getType: DQResultType
}

trait MetricResult extends TypedResult {
  val metricId: String
  val sourceDate: String
  val name: String
  val result: Double
  val additionalResult: String
  val sourceId: String
}

case class ColumnMetricResult(
    metricId: String,
    sourceDate: String,
    name: String,
    sourceId: String,
    columnNames: Seq[String],
    params: String,
    result: Double,
    additionalResult: String
) extends MetricResult {
  override def getType: DQResultType = DQResultTypes.column
}

case class FileMetricResult(
    metricId: String,
    sourceDate: String,
    name: String,
    sourceId: String,
    result: Double,
    additionalResult: String
) extends MetricResult {
  override def getType: DQResultType = DQResultTypes.file
}

case class ComposedMetricResult(
    metricId: String,
    sourceDate: String,
    name: String,
    sourceId: String,
    formula: String,
    result: Double,
    additionalResult: String
) extends MetricResult {
  override def getType: DQResultType = DQResultTypes.composed
}
