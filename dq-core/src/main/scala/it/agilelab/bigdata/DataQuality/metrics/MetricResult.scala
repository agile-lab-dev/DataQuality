package it.agilelab.bigdata.DataQuality.metrics

/**
  * Created by Gianvito Siciliano on 29/12/16.
  *
  * Representations of different metric results
  */
trait TypedResult {
  def getType: String
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
  override def getType = "Column"
}

case class FileMetricResult(
    metricId: String,
    sourceDate: String,
    name: String,
    sourceId: String,
    result: Double,
    additionalResult: String
) extends MetricResult {
  override def getType: String = "File"
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
  override def getType: String = "Composed"
}
