package it.agilelab.bigdata.DataQuality.metrics

import java.text.SimpleDateFormat
import java.util.Calendar

/**
  * Created by Gianvito Siciliano on 29/12/16.
  */



trait MetricResult {
  val id: String
  val name: String
  val result: Double
  val execDate: String = {
    val format = new SimpleDateFormat("yyyy-MM-dd")
    format.format(Calendar.getInstance().getTime)
  }
  val sourceId: String
  def getType: String
}

case class ColumnMetricResult(
                               id:String,
                               name: String,
                               sourceId: String,
                               columnName: String,
                               result: Double
                             ) extends MetricResult {
  override def getType = "Column"
}

case class FileMetricResult(
                             id:String,
                             name: String,
                             sourceId: String,
                             result: Double
                           ) extends MetricResult {
  override def getType: String = "File"
}

case class ComposedMetricResult(
                               id: String,
                               name: String,
                               sourceId: String,
                               formula: String,
                               result: Double
                               ) extends MetricResult {
  override def getType: String = "Composed"
}


